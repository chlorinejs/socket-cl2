(load-file "./shared.cl2")

(defmacro defsocket
  "Sets up a websocket/sockjs object, make it ready for def-socket-handler.
  Socket-object can be a WebSocket or SockJS instance."
  [socket-object & [{:keys [as on-open on-close]}]]
  (let [conn# (or as (gensym "socket"))]
    `(do
       (def ~conn# ~socket-object)
       (defn send-response
         [msg-type data]
         (. ~conn# (send (serialize [msg-type data]))))

       (set! (. ~conn# -emit) send-response)

       (set! (. ~conn# -onopen)
             ~(or on-open
                  `#(println "Welcome to socket")))
       (set! (. ~conn# -onclose)
             ~(or on-close
                  `#(println "Socket: Goodbye!")))
       (set! (. ~conn# -onmessage)
             (fn [e]
               (let [[msg-type data] (deserialize e.data)
                     handler
                     (or (get @socket-atom msg-type)
                         (get @socket-atom :default)
                         #(println "Unknown handler for data of type"
                                   msg-type))]
                 (handler msg-type data send-response ~conn#)))))))
