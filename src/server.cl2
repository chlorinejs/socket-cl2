(defmacro defsocket
  "Sets up a sockjs object, make it ready for def-socket-handler.
  Socket-object is a sockjs server instance created by calling
  createServer method against sockjs module:
  (. sockjs createServer)"
  [socket-sym socket-object
   & [{:keys [on-open on-close debug
              load-balancer-workaround?]}]]
  `(def ~socket-sym
     (let [socket-object# ~socket-object]
       ;; Stores current client's connections.
       (def socket-clients (atom {}))

       ;; An atom that stores sockets' handlers
       (def socket-handlers (atom {}))

       (defn whisper
         "Sends messages to a single client."
         [msg-type data id]
         (when (contains? @socket-clients id)
           (. (get @socket-clients id)
              (write (serialize [msg-type data])))))

       (defn broadcast
         "Sends messages to many clients. A vector of excluded ids
  can be specified."
         [msg-type data & exclusions]
         (doseq [[id client] @socket-clients
                 :when (not (contains? (set exclusions) id))]
           (. client
              (write (serialize [msg-type data])))))

       (defn add-handler
         "Associates a handler for socket's data of a specified type.
  Use `:default` as type to handle data of unknown types.
  A handler is a function that takes four arguments: msg-type, msg-data,
  `respond` function and connection object.
  Use the respond function only if you want to send a response
  message back to the initial message's source.
  `respond` takes two arguments: msg-type and msg-data."
         [msg-type handler]
         ~(when debug `(println "[Socket] Registering handler for "
                                msg-type))
         (swap! socket-handlers
                #(assoc % msg-type handler)))

       ;; Things to do when there's a new connection
       (. socket-object#
          (on "connection"
              (fn [conn#]
                ~(when debug
                   `(println "[Socket] Oopps... connection"))
                (defn respond
                  "The third argument passed to handler to send a
  message back to the client that caused initial message."
                  [res-type res-data]
                  (.write conn#
                          (serialize [res-type res-data])))

                ;; Add the connection to known list
                (swap! socket-clients
                       #(assoc % (:id conn#) conn#))
                ~(when debug
                   `(println "[Socket] All clients: "
                             (keys @socket-clients)))

                ~(when load-balancer-workaround?
                   `(add-interval
                     timer# 15000
                     (try
                       (.. conn# -_session -recv didClose)
                       (catch x nil))))

                (let [on-open
                      ~(or on-open
                           `(fn [_ conn]
                              (println "[Socket] New connection: " (:id conn))))]
                  ;; Calls on-open
                  (on-open respond conn#))

                (let [on-close
                      ~(or on-close
                           `(fn [conn]
                              (println "[Socket] Connection closed: " (:id conn))))]
                  (.on conn# "close"
                       (fn []
                         ~(when load-balancer-workaround?
                            `(remove-interval timer#))
                         ;; Removes the connection from known list
                         (swap! socket-clients
                                #(dissoc % (:id conn#)))
                         ;; Calls on-close
                         (on-close conn#))))
                ;; Top-level data handler
                ;;
                (.on conn# "data"
                     (fn [msg]
                       (let [[msg-type data] (deserialize msg)
                             handler
                             (or (get @socket-handlers msg-type)
                                 (get @socket-handlers :default)
                                 #(println "[Socket] Unknown handler for data of type"
                                           msg-type))]
                         ~(when debug
                            `(println "[Socket] Got a message of" msg-type "type"))
                         (handler msg-type data respond conn#)))))))
       ;; Public API
       { ;; Functions
        :on add-handler
        :whisper whisper
        :broadcast broadcast
        ;; Installs sockjs to a http server instance
        :install (fn [server options]
                   (.installHandlers socket-object# server options))
        ;; Atoms
        :clients socket-clients
        :handlers socket-handlers})))
