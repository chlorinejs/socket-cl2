# Socket-Cl2

Websocket/Sockjs wrappers/macros for ChlorineJS.

# Usage

Socket-Cl2 provides two major files: `client.cl2` and `server.cl2`.
You may want to use one or both of them.

Pull contrib-cl2 to your machine:
```
npm install socket-cl2
```

## Client side
```clojure
;; socket-cl2 requires json.cl2
(load-file "./path/to/node_modules/cl2-contrib/src/json.cl2")
(load-file "./path/to/node_modules/socket-cl2/src/client.cl2")

(defsocket (WebSocket.))
;; or you may prefer sockjs to websocket
;; (defsocket (SockJS.))

(defn welcome-handler [conn msg-type data]
  (. conn send :confirm "Got a welcome message.")
(defsocket-handler :welcome welcome-handler))
```

## Server side (Nodejs + sockjs)
```clojure
;; socket-cl2 requires json.cl2
(load-file "./path/to/node_modules/cl2-contrib/src/json.cl2")
(load-file "./path/to/node_modules/socket-cl2/src/server.cl2")

(require ["sockjs"])
(def socket (. sockjs (createServer)))
(defsocket socket)

(defn init-handler
  "Says welcome to clients."
  [conn msg-type data]
  (broadcast :new-client "Someone new" #{1 2})
  (whisper   :welcome "You're welcome" conn.id))

(defsocket-handler :init init-handler)
```

# License

Copyright © 2013 Hoang Minh Thang

Socket-cl2 library may be used under the terms of either the [GNU Lesser General Public License (LGPL)](http://www.gnu.org/copyleft/lesser.html) or the [Eclipse Public License (EPL)](http://www.eclipse.org/legal/epl-v10.html). As a recipient of Socket-cl2, you may choose which license to receive the code under.
