(ns clj-test-containers.core
  (:require
   [clj-test-containers.interfaces :as i]
   [clj-test-containers.spec.container :as spec.container]
   [clj-test-containers.spec.core :as spec]
   [clojure.string :as str]
   [orchestra.core :refer [defn-spec]])
  (:import
   (java.nio.file
    Paths)
   (org.testcontainers.containers
    BindMode
    GenericContainer
    Network)
   (org.testcontainers.containers.output
    ToStringConsumer)
   (org.testcontainers.containers.wait.strategy
    Wait)
   (org.testcontainers.images.builder
    ImageFromDockerfile)
   (org.testcontainers.utility
    MountableFile)))

(defn-spec ^:private resolve-bind-mode #(instance? BindMode %)
  ^BindMode [bind-mode ::spec/mode]
  (if (= :read-write bind-mode)
    BindMode/READ_WRITE
    BindMode/READ_ONLY))

(defmulti wait
  "Sets a wait strategy to the container.  Supports :http, :health and :log as
  strategies.

  ## HTTP Strategy
  The :http strategy will only accept the container as initialized if it can be
  accessed via HTTP. It accepts a path, a port, a vector of status codes, a
  boolean that specifies if TLS is enabled, a read timeout in seconds and a map
  with basic credentials, containing username and password. Only the path is
  required, all others are optional.

  Example:

  ```clojure
  (wait {:wait-strategy :http
         :port 80
         :path \"/\"
         :status-codes [200 201]
         :tls true
         :read-timeout 5
         :basic-credentials {:username \"user\"
                             :password \"password\"}}
        container)
  ```

  ## Health Strategy
  The :health strategy only accepts a true or false value. This enables support
  for Docker's healthcheck feature, whereby you can directly leverage the
  healthy state of your container as your wait condition.

  Example:

  ```clojure
  (wait {:wait-strategy :health :true} container)
  ```

  ## Log Strategy
  The :log strategy accepts a message which simply causes the output of your
  container's log to be used in determining if the container is ready or not.
  The output is `grepped` against the log message.

  Example:

  ```clojure
  (wait container
        {:wait-strategy :log
         :message \"accept connections\"})
  ```"
  (fn [_container options] (:wait-strategy options)))

(defn-spec ^:private wait-for-http any?
  [^GenericContainer container ::spec.container/container
   options :clj-test-containers.spec.container.wait/http]
  (let [{:keys [path
                port
                status-codes
                tls
                read-timeout
                basic-credentials]} options

        for-http (Wait/forHttp path)]
    (when port
      (.forPort for-http port))

    (doseq [status-code status-codes]
      (.forStatusCode for-http status-code))

    (when tls
      (.usingTls for-http))

    (when read-timeout
      (.withReadTimeout for-http (java.time.Duration/ofSeconds read-timeout)))

    (when basic-credentials
      (let [{username :username password :password} basic-credentials]
        (.withBasicCredentials for-http username password)))

    (.waitingFor container for-http)

    {:wait-for-http (dissoc options :wait-strategy)}))

(defmethod wait :http
  [container options]
  (wait-for-http container options))

(defn-spec ^:private wait-for-health any?
  [^GenericContainer container ::spec.container/container
   _options :clj-test-containers.spec.container.wait/health]
  (.waitingFor container (Wait/forHealthcheck))
  {:wait-for-healthcheck true})

(defmethod wait :health
  [^GenericContainer container options]
  (wait-for-health container options))

(defn-spec ^:private wait-for-log any?
  [^GenericContainer container ::spec.container/container
   {:keys [message]} :clj-test-containers.spec.container.wait/log]
  (let [log-message (str ".*" message ".*\\n")]
    (.waitingFor container (Wait/forLogMessage log-message 1))
    {:wait-for-log-message log-message}))

(defmethod wait :log
  [container options]
  (wait-for-log container options))

(defmethod wait :default [_ _] nil)

(defmulti log
  "Sets a log strategy on the container as a means of accessing the container
  logs.  It currently only supports a :string as the strategy to use.

  ## String Strategy
  The :string strategy sets up a function in the returned map, under the `log`
  key. This function enables the dumping of the logs when passed to the
  `dump-logs` function.

  Example:

  ```clojure
  {:log-strategy :string}
  ```

  Then, later in your program, you can access the logs thus:

  ```clojure
  (def container-config (tc/start! container))
  (tc/dump-logs container-config)
  ```
   "
  (fn [_container options] (:log-strategy options)))

(defn-spec ^:private ^:no-gen log-string ::spec.container/log
  [^GenericContainer container ::spec.container/container]
  (let [to-string-consumer (ToStringConsumer.)]
    (.followOutput container to-string-consumer)
    #(-> (.toUtf8String to-string-consumer)
         (str/replace #"\n+" "\n"))))

(defmethod log :string
  [container _]
  (log-string container))

(defmethod log :slf4j [_ _] nil) ;; Not yet implemented

(defmethod log :default [_ _] nil) ;; Not yet implemented

(declare map->StartedContainer)

(defrecord StoppedContainer
  [^GenericContainer container
   command
   docker-file
   env-vars
   exposed-ports
   host
   image-name
   log-to
   network
   network-aliases
   wait-for
   wait-for-http
   wait-for-healthcheck
   wait-for-log-message]

  i/StoppedContainer

  (map-classpath-resource!
    [this {:keys [^String resource-path ^String container-path mode]}]
    (assoc this
           :container
           (.withClasspathResourceMapping container
                                          resource-path
                                          container-path
                                          (resolve-bind-mode mode))))


  (bind-filesystem!
    [this {:keys [^String host-path ^String container-path mode]}]
    (assoc this
           :container
           (.withFileSystemBind container
                                host-path
                                container-path
                                (resolve-bind-mode mode))))


  (start!
    [this]
    (.start container)
    (let [id           (.getContainerId container)
          map-ports-xf (map #(vector % (.getMappedPort container %)))
          mapped-ports (into {} map-ports-xf exposed-ports)
          logger       (log container log-to)]
      (-> this
          (assoc :id id :mapped-ports mapped-ports :log logger)
          (dissoc :log-to)
          map->StartedContainer)))


  i/CopyFileToContainer

  (copy-file-to-container!
    [this mountable-file container-path]
    (assoc this
           :container
           (.withCopyFileToContainer container
                                     ^MountableFile mountable-file
                                     ^String container-path))))

(defrecord StartedContainer
  [^GenericContainer container
   command
   docker-file
   env-vars
   exposed-ports
   id
   image-name
   mapped-ports
   log
   network
   network-aliases
   wait-for
   wait-for-http
   wait-for-healthcheck
   wait-for-log-message]

  i/StartedContainer

  (execute-command!
    [this command]
    (let [result (.execInContainer container (into-array command))]
      {:exit-code (.getExitCode result)
       :stdout    (.getStdout result)
       :stderr    (.getStderr result)}))


  (dump-logs
    [this]
    (log))


  (stop!
    [this]
    (.stop container)
    (-> this
        (dissoc :id :log :mapped-ports)
        map->StoppedContainer))


  i/CopyFileToContainer

  (copy-file-to-container!
    [this mountable-file container-path]
    (.copyFileToContainer container
                          ^MountableFile mountable-file
                          ^String container-path)
    this))

(defn-spec init ::spec/stopped-container
  "Sets the properties for a testcontainer instance"
  [options ::spec/init-options]
  (let [{:keys [^GenericContainer container
                exposed-ports
                env-vars
                command
                log-to
                network
                network-aliases
                wait-for]} options]

    (.setExposedPorts container (map int exposed-ports))

    (doseq [[k v] env-vars]
      (.addEnv container k v))

    (when command
      (.setCommand container
                   ^"[Ljava.lang.String;" (into-array String command)))

    (when network
      (.setNetwork container (:network network)))

    (when network-aliases
      (.setNetworkAliases container network-aliases))

    (-> options
        (assoc :exposed-ports (vec (.getExposedPorts container))
               :env-vars      (into {} (.getEnvMap container))
               :host          (.getHost container)
               :log-to        log-to
               :network       network)
        (merge (wait container wait-for))
        map->StoppedContainer)))

(defn-spec create ::spec/stopped-container
  "Creates a generic testcontainer and sets its properties"
  [{:keys [^String image-name] :as options} ::spec/create-options]
  (->> (GenericContainer. image-name)
       (assoc options :container)
       init))

(defn-spec ^:no-gen create-from-docker-file ::spec/stopped-container
  "Creates a testcontainer from a provided Dockerfile"
  [{:keys [docker-file] :as options} ::spec/create-from-docker-file-options]
  (let [path (Paths/get "." (into-array String [docker-file]))]
    (->> (.withDockerfile (ImageFromDockerfile.) path)
         (GenericContainer.)
         (assoc options :container)
         init)))

(defn-spec ^:no-gen map-classpath-resource! ::spec/stopped-container
  "Maps a resource in the classpath to the given container path. Should be
  called before starting the container!"
  [config  ::spec/stopped-container
   options ::spec/map-classpath-resource!-options]
  (i/map-classpath-resource! config options))

(defn-spec ^:no-gen bind-filesystem! ::spec/stopped-container
  "Binds a source from the filesystem to the given container path. Should be
  called before starting the container!"
  [config  ::spec/stopped-container
   options ::spec/bind-filesystem!-options]
  (i/bind-filesystem! config options))

(defn-spec ^:no-gen copy-file-to-container! any?
  "If a container is not yet started, adds a mapping from mountable file to
  container path that will be copied to the container on startup. If the
  container is already running, copy the file to the running container"
  [config  ::spec/container
   options ::spec/copy-file-to-container!-options]
  (let [{:keys [type ^String path container-path]} options

        mountable-file (case type
                         :classpath-resource
                         (MountableFile/forClasspathResource path)
                         :host-path
                         (MountableFile/forHostPath path))]
    (i/copy-file-to-container! config
                               mountable-file
                               container-path)))

(defn-spec ^:no-gen execute-command! ::spec/execute-command-result
  "Executes a command in the container, and returns the result"
  [config  ::spec/started-container
   command ::spec/command]
  (i/execute-command! config command))

(defn-spec ^:no-gen dump-logs string?
  "Dumps the logs found by invoking the function on the :log key"
  [config ::spec/started-container]
  (i/dump-logs config))

(defn-spec ^:no-gen start! ::spec/started-container
  "Starts the underlying testcontainer instance and adds new values to the
  response map, e.g. :id and :first-mapped-port"
  [config ::spec/stopped-container]
  (i/start! config))

(defn-spec ^:no-gen stop! ::spec/stopped-container
  "Stops the underlying container"
  [config ::spec/started-container]
  (i/stop! config))

(defn-spec create-network ::spec.container/network
  "Creates a network. The optional map accepts config values for enabling ipv6
  and setting the driver"
  ([]
   (create-network {}))
  ([{:keys [ipv6 driver]} ::spec/create-network-options]
   (let [builder (Network/builder)]
     (when ipv6
       (.enableIpv6 builder true))

     (when driver
       (.driver builder driver))

     (let [network (.build builder)]
       {:network network
        :name    (.getName network)
        :ipv6    (.getEnableIpv6 network)
        :driver  (.getDriver network)}))))

(def ^:deprecated init-network create-network)
