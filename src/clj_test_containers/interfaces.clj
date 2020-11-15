(ns clj-test-containers.interfaces)

(defprotocol CopyFileToContainer

  (copy-file-to-container!
    [this mountable-file container-path]
    "If a container is not yet started, adds a mapping from mountable file to
    container path that will be copied to the container on startup. If the
    container is already running, copy the file to the running container"))

(defprotocol StoppedContainer

  (map-classpath-resource!
    [this options]
    "Maps a resource in the classpath to the given container path. Should be
    called before starting the container!")

  (bind-filesystem!
    [this options]
    "Binds a source from the filesystem to the given container path. Should be
    called before starting the container!")

  (start!
    [this]
    "Executes a command in the container, and returns the result"))

(defprotocol StartedContainer

  (execute-command!
    [this command]
    "Starts the underlying testcontainer instance and adds new values to the
    response map, e.g. :id and :first-mapped-port")

  (dump-logs
    [this]
    "Dumps the logs found by invoking the function on the :log key")

  (stop!
    [this]
    "Stops the underlying container"))
