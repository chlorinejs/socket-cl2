(defmacro defsocket
  "Sets up a websocket/sockjs object with unified api and
  ability to reconnect. Initializer is a function that returns a
  WebSocket or SockJS instance."
  [socket-sym initializer & [{:keys [on-open on-close
                                     max-retries reconnect-interval
                                     debug]}]]
  `(def ~socket-sym
     (do
       ;; default values
       ~(when debug `(println "[Socket] Debug: ON"))
       (def max-retries ~(or max-retries 5))
       ;; if failed to connect, reconnect after one second.
       (def reconnect-interval ~(or reconnect-interval 1000))

       ;; Stores socket's initializer function and current instance.
       (def socket-store (atom {:init ~initializer}))

       ;; Current socket's status
       ;; can be :closed, :connecting and :connected
       (def status (atom :closed))

       ;; Stores number of attempted retries
       (def retries (atom 0))
       ;; Stores socket's handlers associated with their intended message type.
       ;; Read by socket's overall on-message function
       ;; Handlers are added with add-handler
       (def socket-handlers (atom {}))

       ;; Set to `true` if received a "REJECTED" message from server
       ;; else `false`. If `true`, socket is not allowed to reconnect.
       (def rejected? (atom false))

       (defn respond!
         "Sends message to server."
         [msg-type data]
         (. (:instance @socket-store)
            (send (serialize [msg-type data]))))

       (defn close!
         "Closes current socket instance"
         []
         (.close (:instance @socket-store)))

       (defn on-message
         "Socket's top-level message handler. Decodes raw a message
  from server, finds and calls the handler associated with the
  message type."
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
         "Creates a new websocket/sockjs instance, saves it to
   socket-store and calls forge-socket! on it."
         []
         ~(when debug `(println "Hmm... Trying to connect"))
         (reset! status :connecting)
         (swap! socket-store
                #(assoc % :instance ((:init @socket-store))))
         (forge-socket! (:instance @socket-store)))

       (defn forge-socket!
         "Associates a newly-created websocket/sockjs instance
  with event handlers."
         [socket]
         (set! (. socket -emit) respond!)

         (set! (. socket -onopen)
               (fn []
                 (reset! status :connected)
                 ~(if on-open
                    `(on-open)
                    (when debug
                      `(println "Welcome to socket")))))

         (set! (. socket -onclose)
               (fn []
                 (reset! status :closed)
                 ~(if on-close
                    `(on-close)
                    (when debug
                      `(println "Socket: Goodbye!")))))

         (set! (. socket -onmessage) on-message))

       ;; Watches for changes in status
       (add-watch
        status :reconnect
        (fn [_ _ old-status new-status]
          ;; Connection closed? -> Time to reconnect!
          (when (and (not @rejected?) (= :closed new-status))
            (if (= :connected old-status)
              ;; Hmm... there's must be a problem.
              ;; Let's reconnect quickly
              (connect!)
              ;; else (= :connecting old-status)
              ;; Just failed to reconnect? Wait some time before
              ;; reconnecting
              (do-timeout
               reconnect-interval
               ;; Increase number of attempted retries by 1
               (swap! retries inc)
               ~(when debug `(println "[Socket] Retried number"
                                      @retries))
               ;; Are we allowed to reconnect?
               (if (<= @retries max-retries)
                 (connect!)
                 (println "[Socket] Sorry, max retries reached!")))))))

       ;; Connects for the first time!
       (connect!)

       (defn add-handler
         "Registers a handler to its intended message type."
         [msg-type handler]
         (swap! socket-handlers
                #(assoc % msg-type handler)))

       (add-handler "REJECTED"
                    (fn [_ data]
                      (reset! rejected? true)
                      (println "[Socket] Rejected: " data)
                      (. (:instance @socket-store) close)))
       ;; Public API
       {;; Functions
        :on add-handler
        :emit respond!
        :close close!
        :connect connect!
        ;; Atoms
        :store socket-store
        :status status
        :rejected? rejected?})))
