(def ^{:doc "An atom that stores sockets' handlers"}
  socket-atom (atom {}))

(defmacro defsocket-handler
  "Associates a handler for socket's data of a specified type.
  Use `:default` as type to handle data of unknown types.
  A handler is a function that takes four arguments: msg-type, msg-data,
  send-response function and connection object.
  Use the send-response function only if you want to send a response
  message back to the initial message's source.
  Send-response takes two arguments: msg-type and msg-data.

  For server side, you can also use `broadcast` and `whisper` functions
  to send messages to clients."
  [msg-type handler]
  `(swap! socket-atom (fn [a] (merge a {~msg-type ~handler}))))
