(defmacro defsocket
  [socket-sym initializer & [{:keys [on-open on-close]}]]
  `(def ~socket-sym
     (do
       ;;Todo: channels
       (def socket-store (atom {:init ~initializer}))

       (def status (atom :closed))

       (def socket-handlers (atom {}))

       (def rejected? (atom false))

       (defn respond!
         "Sends message to server."
         [msg-type data]
         (. (:instance @socket-store)
            (send (serialize [msg-type data]))))

       (defn close!
         "Close current socket instance"
         (.close (:instance @socket-store)))

       (def on-open (or ~on-open
                        #(println "Welcome to socket")))

       (def on-close (or ~on-close
                         #(println "Socket: Goodbye!")))

       (defn on-message
         "Decodes raw a message from server, finds and calls the
  handler associated with the message type."
         [e]
         (let [[msg-type data] (deserialize e.data)
               handler
               (or (get @socket-handlers msg-type)
                   (get @socket-handlers :default)
                   #(println "Unknown handler for data of type"
                             msg-type))]
           (handler msg-type data respond!
                    (:instance @socket-store))))

       (defn connect!
         "Creates a new websocket/sockjs, saves it to socket-store
  and calls forge-socket! on it."
         []
         (println "Hmm... Trying to connect")
         (swap! socket-store
                #(assoc % :instance ((:init @socket-store))))
         (forge-socket! (:instance @socket-store)))

       (defn forge-socket!
         "Associates a new websocket/sockjs instance with event
  handlers."
         [socket]
         (set! (. socket -emit) respond!)

         (set! (. socket -onopen)
               (fn []
                 (reset! status :opened)
                 (on-open)))
         (set! (. socket -onclose)
               (fn []
                 (reset! status :closed)
                 (on-close)
                 (when-not @rejected?
                   (connect!))))
         (set! (. socket -onmessage) on-message))

       (connect!)

       (defn add-handler
         "Registers a handler to its intended message type."
         [msg-type handler]
         (swap! socket-handlers
                #(assoc % msg-type handler)))

       (add-handler "REJECTED"
                    (fn [_ data]
                      (reset! rejected? true)
                      (println "Rejected: " data)
                      (. (:instance @socket-store) close)))

       {:on add-handler
        :emit respond!
        :close close!
        :connect connect!
        :store socket-store
        :status status
        :rejected? rejected?})))
