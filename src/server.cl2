(load-file "./shared.cl2")

(def ^{:doc "Stores current client's connections."}
  socket-clients (atom {}))

(defmacro defsocket
  "Sets up a websocket/sockjs object, make it ready for def-socket-handler.
  Socket-object is a sockjs server instance created by calling
  createServer method against sockjs module:
  (. sockjs createServer)"
  [socket-object & [{:keys [as on-open on-close
                            load-balancer-workaround?]}]]
  (let [socket-sym (or as (gensym "socket"))]
    `(do
       (def ~socket-sym ~socket-object)
       (. ~socket-sym
          (on "connection"
              (fn [conn#]
                (defn send-response
                  "The third argument passed to handler to send a
  message back to the client that caused initial message."
                  [response-type response-data]
                  (. conn#
                     (write
                      (serialize [response-type response-data]))))
                (swap! socket-clients
                       #(assoc % (:id conn#) conn#))
                ~(when load-balancer-workaround?
                   `(add-interval
                     timer# 15000
                     (try
                       (.. conn# -_session -recv didClose)
                       (catch x nil))))
                (let [on-open
                      ~(or on-open
                           `(fn [_ conn]
                              (println "New connection: " (:id conn))))]
                  (on-open send-response conn#))
                (let [on-close
                      ~(or on-close
                           `(fn [conn]
                              (println "Connection closed: " (:id conn))))]
                  (.on conn# "close"
                       (fn []
                         ~(when load-balancer-workaround?
                            `(remove-interval timer#))
                         (swap! socket-clients
                                #(dissoc % (:id conn#)))
                         (on-close conn#))))
                (.on conn# "data"
                     (fn [msg]
                       (println "On-data got this" msg)
                       (let [[msg-type data] (deserialize msg)
                             handler
                             (or (get @socket-atom msg-type)
                                 (get @socket-atom :default)
                                 #(println "Unknown handler for data of type"
                                           msg-type))]
                         (handler msg-type data send-response conn#))))))))))

(defn whisper
  "Sends messages to a single client"
  [msg-type data id]
  (if (contains? @socket-clients id)
    (. (get @socket-clients id)
       (write (serialize [msg-type data])))))

(defn broadcast
  "Sends messages to many clients. A vector of excluded ids can be specified"
  [msg-type data & exclusions]
  (doseq [[id client] @socket-clients
          :when (not (contains? (set exclusions) id))]
    (. client
       (write (serialize [msg-type data])))))
