(ns clj-test-containers.spec.container
  (:require
   [clj-test-containers.spec.network :as spec.network]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen])
  (:import
   (org.testcontainers.containers
    GenericContainer)))

(s/def ::container
  (s/with-gen #(instance? GenericContainer %)
              #(gen/fmap (fn [^String image-name] (GenericContainer. image-name))
                         (gen/string-alphanumeric))))

(s/def ::port
  (s/int-in 1 65535))

(s/def ::exposed-ports
  (s/coll-of ::port))

(s/def ::env-vars
  (s/map-of string? string?))

(s/def ::host
  string?)

(s/def ::log
  (s/nilable (s/fspec :args (s/cat)
                      :ret  string?)))

(s/def ::id
  string?)

(s/def ::log-strategy
  #{:string})

(s/def ::path
  string?)

(s/def ::message
  string?)

(s/def ::check
  boolean?)

(s/def ::string
  string?)

(s/def ::mapped-ports
  (s/map-of ::port ::port))

(s/def ::status-codes
  (s/coll-of (s/int-in 100 599)))

(s/def ::tls
  any?)

(s/def ::read-timeout
  (s/double-in :min 1 :max Integer/MAX_VALUE))

(s/def ::username
  string?)

(s/def ::password
  string?)

(s/def ::basic-credentials
  (s/keys :req-un [::username
                   ::password]))

(s/def ::wait-for-http
  (s/keys :opt-un [::path
                   ::port
                   ::status-codes
                   ::tls
                   ::read-timeout
                   ::basic-credentials]))

(s/def :clj-test-containers.spec.container.wait.http/wait-strategy
  #{:http})

(s/def :clj-test-containers.spec.container.wait/http
  (s/merge
   (s/keys
    :req-un [:clj-test-containers.spec.container.wait.http/wait-strategy])
   ::wait-for-http))

(s/def :clj-test-containers.spec.container.wait.health/wait-strategy
  #{:health})

(s/def :clj-test-containers.spec.container.wait/health
  (s/keys
   :req-un [:clj-test-containers.spec.container.wait.health/wait-strategy]))

(s/def :clj-test-containers.spec.container.wait.log/wait-strategy
  #{:log})

(s/def :clj-test-containers.spec.container.wait/log
  (s/keys :req-un [:clj-test-containers.spec.container.wait.log/wait-strategy
                   ::message]))

(s/def ::wait-for
  (s/or :http   :clj-test-containers.spec.container.wait/http
        :health :clj-test-containers.spec.container.wait/health
        :log    :clj-test-containers.spec.container.wait/log))

(s/def ::wait-for-healthcheck
  boolean?)

(s/def ::wait-for-log-message
  string?)

(s/def ::log-to
  (s/nilable (s/keys :req-un [::log-strategy]
                     :opt-un [::string])))

(s/def ::command
  (s/coll-of string?))

(s/def ::network-aliases
  (s/coll-of string?))

(s/def ::image-name
  string?)

(s/def ::docker-file
  string?)

(s/def ::network
  (s/keys :req-un [::spec.network/network
                   ::spec.network/name
                   ::spec.network/ipv6
                   ::spec.network/driver]))
