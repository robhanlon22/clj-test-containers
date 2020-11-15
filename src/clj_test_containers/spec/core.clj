(ns clj-test-containers.spec.core
  (:require
   [clj-test-containers.interfaces :as i]
   [clj-test-containers.spec.container :as spec.container]
   [clj-test-containers.spec.network :as spec.network]
   [clojure.spec.alpha :as s]))

(s/def ::command
  (s/nilable ::spec.container/command))

(s/def ::docker-file
  (s/nilable ::spec.container/docker-file))

(s/def ::image-name
  (s/nilable ::spec.container/image-name))

(s/def ::network
  (s/nilable ::spec.container/network))

(s/def ::network-aliases
  (s/nilable ::spec.container/network-aliases))

(s/def ::wait-for
  (s/nilable ::spec.container/wait-for))

(s/def ::wait-for-http
  (s/nilable ::spec.container/wait-for-http))

(s/def ::wait-for-healthcheck
  (s/nilable ::spec.container/wait-for-healthcheck))

(s/def ::wait-for-log-message
  (s/nilable ::spec.container/wait-for-log-message))

(s/def ::base-container
  (s/keys :req-un [::command
                   ::spec.container/container
                   ::spec.container/env-vars
                   ::docker-file
                   ::spec.container/exposed-ports
                   ::spec.container/host
                   ::image-name
                   ::network
                   ::network-aliases
                   ::wait-for
                   ::wait-for-http
                   ::wait-for-healthcheck
                   ::wait-for-log-message]))

(s/def ::stopped-container
  (s/and (s/merge ::base-container
                  (s/keys :req-un [::spec.container/log-to]))
         #(satisfies? i/StoppedContainer %)))

(s/def ::started-container
  (s/and (s/merge ::base-container
                  (s/keys :req-un [::spec.container/id
                                   ::spec.container/log
                                   ::spec.container/mapped-ports]))
         #(satisfies? i/StartedContainer %)))

(s/def ::container
  (s/or :stopped-container ::stopped-container
        :started-container ::started-container))

(s/def ::base-init-options
  (s/keys :opt-un [::spec.container/command
                   ::spec.container/env-vars
                   ::spec.container/exposed-ports
                   ::spec.container/log-to
                   ::spec.container/network
                   ::spec.container/network-aliases
                   ::spec.container/wait-for]))

(s/def ::init-options
  (s/merge ::base-init-options
           (s/keys :req-un [::spec.container/container])))

(s/def ::create-options
  (s/merge ::base-init-options
           (s/keys :req-un [::spec.container/image-name])))

(s/def ::create-from-docker-file-options
  (s/merge ::base-init-options
           (s/keys ::req-un [::spec.container/docker-file])))

(s/def ::resource-path
  string?)

(s/def ::container-path
  string?)

(s/def ::mode
  (s/or :read-write #{:read-write}
        :any        any?))

(s/def ::map-classpath-resource!-options
  (s/keys :req-un [::resource-path
                   ::container-path
                   ::mode]))

(s/def ::host-path
  string?)

(s/def ::bind-filesystem!-options
  (s/keys :req-un [::host-path
                   ::container-path
                   ::mode]))

(s/def ::type
  #{:classpath-resource :host-path})

(s/def ::path
  string?)

(s/def ::copy-file-to-container!-options
  (s/keys :req-un [::type
                   ::path
                   ::container-path]))

(s/def ::exit-code
  nat-int?)

(s/def ::stdout
  string?)

(s/def ::stderr
  string?)

(s/def ::execute-command-result
  (s/keys :req-un [::exit-code
                   ::stdout
                   ::stderr]))

(s/def ::create-network-options
  (s/keys :opt-un [::spec.network/ipv6
                   ::spec.network/driver]))
