(ns slacker.test.server
  (:require [slacker.server :refer :all]
            [slacker.serialization :refer :all]
            [slacker.common :refer :all]
            [slacker.protocol :as protocol]
            [clojure.test :refer :all]
            [clojure.string :refer [split]]
            [link.core :as link])
  (:import [io.netty.buffer Unpooled]))

(def funcs {"plus" + "minus" - "prod" * "div" /})

(deftest test-server-pipeline
  (let [server-pipeline (build-server-pipeline
                         funcs {:pre identity :before identity :after identity :post identity}
                         (atom {}))
        req {:content-type :nippy
             :data (serialize :nippy [100 0])
             :fname "plus"}
        req2 {:content-type :nippy
              :data (serialize :nippy [100 0])
              :fname "never-found"}
        req3 {:content-type :nippy
              :data (serialize :nippy [100 0])
              :fname "div"}]

    (let [result (server-pipeline req)]
      (is (= :success (:code result)))
      (is (= 100 (deserialize :nippy (:result result)))))

    (let [result (server-pipeline req2)]
      (is (= :not-found (:code result))))

    (let [result (server-pipeline req3)]
      (is (= :exception (:code result))))))

(def interceptor (fn [req] (update-in req [:result] str)))

(deftest test-server-pipeline-interceptors
  (let [server-pipeline (build-server-pipeline
                         funcs {:pre identity :before identity :after interceptor :post identity}
                         (atom {}))
        req {:content-type :nippy
             :data (serialize :nippy [100 0])
             :fname "prod"}]

    (is (= "0" (deserialize :nippy (:result (server-pipeline req)))))))

(deftest test-ping
  (let [request [protocol/v5 [0 [:type-ping]]]
        [_ [_ response]] (handle-request nil request nil nil nil)]
    (is (= :type-pong (nth response 0)))))

(deftest test-invalid-packet
  (let [request [protocol/v5 [0 [:type-unknown]]]
        [_ [_ response]] (handle-request nil request nil nil nil)]
    (is (= :type-error (nth response 0)))
    (is (= :invalid-packet (-> response
                               second
                               first)))))


(deftest test-functions-inspect
  (let [request [protocol/v5 [0 [:type-inspect-req [:functions
                                                    (Unpooled/wrappedBuffer (.getBytes "nil"))]]]]
        [_ [_ [_ [result]]]] (handle-request nil request nil
                                             (build-inspect-handler funcs) nil)
        response (deserialize :clj result)]
    (= (map name (keys funcs)) response)))

(deftest test-parse-functions
  (testing "parsing function map"
    (let [nmap {"example.api2" {"echo2" (constantly "echo2")
                                "echo3" (constantly "echo3")
                                "echo4" (constantly "echo4")}
                "example.api3" {"reverse2" (constantly (reverse "reverse2"))
                                "reverse3" (constantly (reverse "reverse3"))}}
          parsed-results (parse-funcs nmap)]

      (is (= 5 (count parsed-results)))
      (is (= 3 (count (filter #(clojure.string/starts-with? % "example.api2/")
                              (keys parsed-results)))))
      (is (= 2 (count (filter #(clojure.string/starts-with? % "example.api3/")
                              (keys parsed-results)))))
      (is (= "echo2" ((parsed-results "example.api2/echo2"))))
      (is (= (reverse "reverse2") ((parsed-results "example.api3/reverse2")))))))
