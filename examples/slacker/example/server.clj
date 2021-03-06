(ns slacker.example.server
  (:use [slacker server interceptor])
  (:use slacker.interceptors.stats)
  (:use slacker.interceptors.exectime)
  (:use slacker.interceptors.logargs)
  (:require [slacker.example.api]
            [slacker.serialization.nippy]))

(definterceptor log-function-calls
  :before (fn [req]
            (println (str (-> req :client :remote-addr)  " calling: " (:fname req)))
            req))

(defn -main [& args]
  (let [server (start-slacker-server [(the-ns 'slacker.example.api)
                                      {"slacker.example.api2" {"echo2" (fn [n] n)}}]
                                     2104
                                     :http 4104)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable
                               (fn []
                                 (println "About to shutting down slacker server")
                                 (stop-slacker-server server)
                                 (println "Server stopped."))))
    (println "Slacker example server started on port 2104, http enabled on 4104")))
