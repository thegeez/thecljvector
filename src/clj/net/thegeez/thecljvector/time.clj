(ns net.thegeez.thecljvector.time
  (:require [clj-time.core :as time]
            [clj-time.coerce :as time-coerce])
  (:import [org.joda.time Period]
           [org.joda.time.format PeriodFormatter PeriodFormatterBuilder]))

;; h/t http://stackoverflow.com/a/2796052

(def ^org.joda.time.format.PeriodFormatter period-formatter
  (-> (org.joda.time.format.PeriodFormatterBuilder.)
      .appendYears
      (.appendSuffix " years, ")
      .appendMonths
      (.appendSuffix " months, ")
      .appendWeeks
      (.appendSuffix " weeks, ")
      .appendDays
      (.appendSuffix " days, ")
      .appendHours
      (.appendSuffix " hours, ")
      .appendMinutes
      (.appendSuffix " minutes, ")
      .appendSeconds
      (.appendSuffix " seconds")
      .printZeroNever
      .printZeroRarelyLast
      .toFormatter))

(defn time-ago-tags
  ([from]
   (time-ago-tags from (time/now)))
  ([from to]
   (let [from (time-coerce/from-long from)
         period (org.joda.time.Period. from to)
         period-str (.print period-formatter period)
         ;; don't know how to only print most significant unit with joda time
         period-str (first (clojure.string/split period-str #","))
         period-str (if (seq period-str)
                      (str period-str " ago")
                      (str "moments ago"))]
     [:span {:title from}
      period-str])))
