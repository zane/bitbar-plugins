(ns io.zane.server
  (:require [org.httpkit.server :as server]))

(defn one-shot
  "Launches a web server that accepts one request, blocks waiting for a
  response, and then returns that response."
  [{:keys [port timeout] :or {port 8090 timeout 20000}}]
  (let [promise (promise)
        handler (fn [{:keys [query-string] :as request}]
                  (let [query-params (-> query-string )
                        request (assoc request :query-params query-params)]
                    (deliver promise request))
                  {:status 200
                   :body "Success! You can close this window now."})
        stop-server! (server/run-server handler {:port port :thread 1})
        result (deref promise timeout ::timeout)]
    (stop-server!)
    (case result
      ::timeout (throw (ex-info "timed out" {}))
      result)))
