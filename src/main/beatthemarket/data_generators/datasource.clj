(ns beatthemarket.data-generators.datasource
  (:require [beatthemarket.data-generators.core :as core]
            [beatthemarket.data-generators.analytics :as analytics]
            ;; [clojure.math.numeric-tower :as math]
            )
  (:import [org.apache.commons.math3.distribution BetaDistribution]))


;; ==>
#_(defn polynomial [a b c x]
    (->
      (+ (* a
            (Math/pow x 3))

         (* b
            (Math/pow x 2)))
      (- (* c x))))

(defn polynomial [a b c x]
  (-> (- (* a (Math/pow x 2))
         (* b (Math/pow x 1)))
      (- (* c x))))

(defn polynomial-xintercept [x]
  (polynomial 2 2 3 x))

(defn polynomial-redux [x]

  ;; +1 (x^5)
  ;; -8 (x^3)
  ;; +10 (x^1)
  ;; +6

  (+ (* 1 (Math/pow x 5))
     (* -8 (Math/pow x 3))
     (* 10 (Math/pow x 1))
     (* 6  (Math/pow x 0))))


;; ==>
;; f(x) = a sin (b(x − c)) + d
;; y = a sin (b(x − c)) + d
;; y = a sin (b(x − pi/2)) + d

;; d - quantifies vertical translation
;; a - amplitude of the wave
;; b - horizontal dilation
;; c - horizontal translation
(defn sine [a b d x]
  (- (* a
        (Math/sin (* b
                     (- x
                        (/ Math/PI 2)))))
     d))

(defn sine-xintercept [x]
  (sine 2 2 0 x))


;; ==>
(defn ydirection [ypos]
  (if (pos? ypos)
    :positive
    :negative))

(defn direction-changed? [ypos dirn]
  (not (= (ydirection ypos)
          dirn)))

(defn get-opposite-direction-key [ydir]
  (if (= ydir :positive)
    :negative
    :positive))

(defn get-opposite-direction-fn [dirn]
  (if (= dirn +) - +))

(defn effectively-zero? [xval]
  (= 0.0 (Double. (format "%.7f" xval))))

(defn find-xintercept [direction mfn]

  ;; find initial direction of y (take y at 0.1 of x=0)
  ;; step 1 in given direction until we pass through x
  (loop [start-point 0.0
         distance    1.0
         ydir        (ydirection (mfn (direction 0 0.1)))
         dirn        direction]

    (let [next-point (dirn start-point distance)]

      (if (effectively-zero? (mfn next-point))

        next-point

        (let [dc? (direction-changed? (mfn next-point) ydir)]

          ;; (println (str "start[" start-point "] / end[" next-point "] / direction-changed?[" dc? "] / Y[" (mfn next-point) "]"))
          ;; divide that step in half and go the other direction, until we again pass through x
          ;; repeat until we have a zero up to the 7th digit
          (recur next-point
                 (if dc? (/ distance 2) distance)
                 (if dc? (get-opposite-direction-key ydir) ydir)
                 (if dc? (get-opposite-direction-fn dirn) dirn)))))))


;; phases - figuring out x intercepts
;; repeatable
;;   mark phase start / end
;; => randomize phase entry point
;; => randomize sample length of phase (between 50% - 100%)

#_(comment

  ;; find x-intercepts both left and right from x=0.
  (find-xintercept - polynomial-xintercept)
  -1.8228756561875343

  (find-xintercept + polynomial-xintercept)
  0.8228756487369537

  (find-xintercept - sine-xintercept)
  -1.570796325802803

  (find-xintercept + sine-xintercept)
  1.570796325802803)


;; stolen from lazytest - no longer under active development
;; https://github.com/stuartsierra/lazytest/blob/master/modules/lazytest/src/main/clojure/lazytest/random.clj
(defn rand-double-in-range
  "Returns a random double between min and max."
  [min max]
  {:pre [(<= min max)]}
  (+ min (* (- max min) (Math/random))))


;; vertical dilation
;; => randomize vertical dilation (or amplitude)
;;   polynomial: 2 - 0.5
;;   sine: 0.5 - 2.7

(defn randomize-vertical-dilation-P [x]  ;; [x mfn [min' max']]
  (let [a (rand-double-in-range 0.5 2)]
    (polynomial a 2 3 x)))

(defn randomize-vertical-dilation-S [x]
  (let [a (rand-double-in-range 0.5 2.7)]
    (sine a 2 0 x)))


(defn randomize-vertical-dilation [mathfn min' max']
  (let [a (rand-double-in-range min' max')]
    (partial mathfn a)))


;; horizontal dilation
;; => randomize horizontal dilation
;;   polynomial: between 2 - 0.5 (larger b yields a narrower curve)
;;   sine: 2.7 - 0.3

(defn randomize-horizontal-dilation [mathfn-curried min' max']
  (let [b (rand-double-in-range min' max')]
    (partial mathfn-curried b)))


;; granularity
;; => randomize granularity, or zoom of sample
;;   polynomial:  between 0.1 - 1
;;   sine:  between 0.1 - 1


;; combination
;;   (phase begin / end)
;;   (beta curve) Sine + Polynomial + Stochastic Oscillating, distributed under a Beta Curve
;; => beta distribution of a=2 b=4.1 x=0 (see: http://keisan.casio.com/exec/system/1180573226)

(defn test-beta [beta-distribution]
  (let [sample-val (.sample beta-distribution)]
    (cond
     (< sample-val 0.33) :a
     (< sample-val 0.66) :b
     :else :c)))

(defn generate-polynomial-sequence []

  (let [one (randomize-vertical-dilation polynomial 0.5 2)
        two (randomize-horizontal-dilation one 0.5 2)
        polyn-partial (partial two 3)

        xinterc-polyn-left (find-xintercept - polynomial-xintercept)
        xinterc-polyn-right (find-xintercept + polynomial-xintercept)

        granularityP (rand-double-in-range 0.1 1)
        xsequenceP (iterate (partial + granularityP) xinterc-polyn-left)]

    (map polyn-partial xsequenceP)))

(defn generate-sine-sequence []

  (let [ein (randomize-vertical-dilation sine 0.5 2.7)
        zwei (randomize-horizontal-dilation ein 0.3 2.7)
        sine-partial (partial zwei 0)

        xinterc-sine-left (find-xintercept - sine-xintercept)
        xinterc-sine-right (find-xintercept + sine-xintercept)

        granularityS (rand-double-in-range 0.1 1)
        xsequenceS (iterate (partial + granularityS) xinterc-sine-left)]

    (map sine-partial xsequenceS)))

(defn generate-oscillating-sequence []

  (analytics/generate-prices-without-population 5 15))


(defn sample-dispatcher [sample-type sample-length sample-fn]

  (let [sample-seq (take sample-length (sample-fn))]

    ;;(println (str "Generating [" sample-type "] sample / length[" sample-length "]"))
    ;;(println sample-seq)
    ;;(println)

    sample-seq))

(defn sample-prices [beta-distribution]

  ;; [ok] have a sequence that iteratively calls the below sample `let`
  ;; [ok] randomize length of each sample

  ;; start-point
  ;; move next sequence to endpoint of previous
  ;; ... reduce / cat

  (let [sample-val (.sample beta-distribution)]
    (cond
      (< sample-val 0.33) (sample-dispatcher :sine (rand-double-in-range 10 15) generate-sine-sequence)
      (< sample-val 0.66) (sample-dispatcher :polynomial (rand-double-in-range 4 6) generate-polynomial-sequence)
      :else               (sample-dispatcher :oscillating (rand-double-in-range 8 10) generate-oscillating-sequence))))


(defn generate-prices-orig [beta-distribution]

  (reduce (fn [^clojure.lang.LazySeq rslt
              ^clojure.lang.LazySeq each-sample-seq]

            (let [beginning-price (if (empty? rslt)
                                    (rand-double-in-range 5 15)
                                    (last rslt))
                  sample-seq-head (first each-sample-seq)
                  price-difference (Math/abs (- sample-seq-head beginning-price))]

              ;; only raise the price if below the beginning price
              (if (< sample-seq-head beginning-price)
                (concat rslt (map #(+ % price-difference) each-sample-seq))
                (concat rslt (map #(- % price-difference) each-sample-seq) each-sample-seq))))
          '()
          (repeatedly #(sample-prices beta-distribution))))


(defn generate-prices-iterate [beta-distribution]

  (let [sample-seq (repeatedly #(sample-prices beta-distribution))

        iterfn (fn [[^clojure.lang.LazySeq rslt
                    ^clojure.lang.LazySeq remaining-sample-seq]]

                 (let [each-sample-seq (first remaining-sample-seq)
                       beginning-price (if (empty? rslt)
                                         (rand-double-in-range 5 15)
                                         (last rslt))
                       sample-seq-head (first each-sample-seq)
                       price-difference (Math/abs (- sample-seq-head beginning-price))]

                   ;; only raise the price if below the beginning price
                   (if (< sample-seq-head beginning-price)

                     [(concat rslt (map #(+ % price-difference) each-sample-seq))
                      (rest remaining-sample-seq)]
                     [(concat rslt (map #(- % price-difference) each-sample-seq))
                      (rest remaining-sample-seq)])))]

    (map first (iterate iterfn ['() sample-seq]))))


(defn generate-prices-for [beta-distribution]

  (def previous-price nil)

  (let [adjusted-samples (for [each-sample-seq (repeatedly #(sample-prices beta-distribution))
                                :let [beginning-price (if (nil? previous-price)
                                                        (rand-double-in-range 5 15)
                                                        previous-price)

                                      sample-seq-head (first each-sample-seq)
                                      price-difference (Math/abs (- sample-seq-head beginning-price))

                                      adjusted-sample (if (< sample-seq-head beginning-price)
                                                        (map #(+ % price-difference) each-sample-seq)
                                                        (map #(- % price-difference) each-sample-seq))

                                      _ (alter-var-root #'previous-price (fn [x] (last adjusted-sample)))]]

                           adjusted-sample)]

    (apply concat adjusted-samples)))


(defn generate-prices-partition [beta-distribution]

  (let [samples-sequence (repeatedly #(sample-prices beta-distribution))
        partitioned-sequences (partition 2 1 samples-sequence)

        mapping-fn (fn [[fst snd]]

                     (let [beginning-price (last fst)
                           sample-seq-head (first snd)
                           price-difference (Math/abs (- sample-seq-head beginning-price))]

                       (if (< sample-seq-head beginning-price)
                         (concat fst (map #(+ % price-difference) snd))
                         (concat fst (map #(- % price-difference) snd)))))]

    (apply concat (map mapping-fn partitioned-sequences))))


(defn generate-prices-reductions [beta-distribution]

  (reductions (fn [^clojure.lang.LazySeq rslt
                  ^clojure.lang.LazySeq each-sample-seq]

                (let [beginning-price (if (empty? rslt)
                                        (rand-double-in-range 5 15)
                                        (last rslt))
                      sample-seq-head (first each-sample-seq)
                      price-difference (Math/abs (- sample-seq-head beginning-price))]

                  ;; only raise the price if below the beginning price
                  (if (< sample-seq-head beginning-price)
                    (concat rslt (map #(+ % price-difference) each-sample-seq))
                    (concat rslt (map #(- % price-difference) each-sample-seq) each-sample-seq))))
              '()
              (repeatedly #(sample-prices beta-distribution))))


(defn generate-prices
  ([] (generate-prices (BetaDistribution. 2.0 4.1)))
  ([beta-distribution]

   (map (fn [x]
          (if (neg? x) (* -1 x) x))
        (distinct
         (apply concat (generate-prices-reductions beta-distribution))))))


(defmethod print-method clojure.lang.PersistentQueue
  [q, w]
  (print-method '<- w) (print-method (seq q) w) (print-method '-< w))

(defn sine+cosine [x]
  (+ (Math/sin x)
     (Math/cos
       (* (Math/sqrt 3)
          x))))

(defn generate-cosine-sequence []
  (map #(Math/cos
          (* (Math/sqrt 3)
             %))
       (range)))

(defn generate-oscillating-sequence []
  (->> (core/generate-prices 15 20)
       (map :last)))


;; TODO Cleanup
;; put into fn
;; make configurable, which algorithms to combine
;; stop oscillating from going from i. vanishing or ii. exploding
(comment

  (require '[clojure.data.csv :as csv]
           '[clojure.java.io :as io])

  (with-open [output-writer (io/writer "out.combined.csv")]
    (let [length 128
          xaxis (range)

          ysines (generate-sine-sequence)
          yconsines (generate-cosine-sequence)
          yoscillatings (generate-oscillating-sequence)
          yaxis (map (fn [& args]
                       (apply + args))
                     ysines
                     yconsines
                     yoscillatings)]

      (->> (interleave xaxis yaxis)
           (partition 2)
           (take length)
           (csv/write-csv output-writer)))))


;; NOTE

;; Beta Distribution for these things
;; A. Direction of price changes
;;   Level 1: More increasing price changes than decreasing
;;   Level 5: Equal swings between increasing price changes -> decreasing
;;   Level 10: More decreasing price changes than increasing

;; B. Occurence of high vs low price changes
;;   Level 1: Big price changes
;;   Level 10: Small price changes

;; C. Number of ticks before changing beta distribution for direction of price change
;;   Level 1: Midpoint distribution for when to change price direction

;; D Start Price
(comment

  ;; Greater price swings at opposite ends
  ;; alpha = beta = 0.5

  ;; Prices within 0.25 / 0.75
  ;; alpha = beta = 2.0

  ;; Prices closer to the high end
  ;; alpha = 5 / beta = 1

  ;; Prices closer to the low end
  ;; alpha = 1 / beta = 3
  ;; alpha = 1 / beta = 5

  ;; https://en.wikipedia.org/wiki/Beta_distribution

  (def beta-distribution (BetaDistribution. 2.0 2.0))
  (def betas (repeatedly #(.sample beta-distribution)))

  (require '[clojure.data.csv :as csv]
           '[clojure.java.io :as io])

  (with-open [writer-midpoint (io/writer "out.beta-midpoint.csv")
              writer-bigswings (io/writer "out.beta-bigswings.csv")
              writer-highend (io/writer "out.beta-highend.csv")
              writer-lowend-a (io/writer "out.beta-lowend-a.csv")
              writer-lowend-b (io/writer "out.beta-lowend-b.csv")]
    (let [length 128
          xaxis (range)

          beta-midpoint (BetaDistribution. 2.0 2.0)
          yaxis (repeatedly #(.sample beta-midpoint))

          beta-bigswings (BetaDistribution. 0.5 0.5)
          ybigswings (repeatedly #(.sample beta-bigswings))

          beta-highend (BetaDistribution. 3 1)
          yhighend (repeatedly #(.sample beta-highend))

          beta-loend-a (BetaDistribution. 1 3)
          ylowend-a (repeatedly #(.sample beta-loend-a))

          beta-lowend-b (BetaDistribution. 1 5)
          ylowend-b (repeatedly #(.sample beta-lowend-b))]

      (->> (interleave xaxis yaxis)
           (partition 2)
           (take length)
           (csv/write-csv writer-midpoint))

      (->> (interleave xaxis ybigswings)
           (partition 2)
           (take length)
           (csv/write-csv writer-bigswings))

      (->> (interleave xaxis yhighend)
           (partition 2)
           (take length)
           (csv/write-csv writer-highend))

      (->> (interleave xaxis ylowend-a)
           (partition 2)
           (take length)
           (csv/write-csv writer-lowend-a))

      (->> (interleave xaxis ylowend-b)
           (partition 2)
           (take length)
           (csv/write-csv writer-lowend-b)))))


;; NOTE

;; BetaDistribution for these parameters (both Sine + Polynomial)

;; A. Vertical

;; B. Wave Length
;;   Level 1: i. Larger wave lengths (WL);
;;            ii. WL dilation is smaller;
;;            iii. Less occurrence of WL dilation
;;   Level 10: i. medium WLs
;;             ii. WL dilation is medium
;;             iii. Larger occurence of wave length dilation
(comment

  ;; (def bdist1 (BetaDistribution. 2.0 5.0))
  ;; (def bdist2 (BetaDistribution. 2.0 4.0))
  ;; (def bdist3 (BetaDistribution. 2.0 3.0))
  ;; (def bdist-even (BetaDistribution. 2.0 2.0))

  (def bdist (BetaDistribution. 2.0 4.1))
  (def result (repeatedly #(test-beta bdist)))
  (sort (take 100 result))

  (take 5 (generate-prices-orig bdist))
  (take 20 (generate-prices-iterate bdist))
  (take 2 (generate-prices-for bdist))
  (take 2 (generate-prices-partition bdist))

  (last (take 20 (generate-prices-reductions bdist)))
  (take 100 (generate-prices))


  (def sine-sequence (generate-sine-sequence))
  (def polynomial-sequence (generate-polynomial-sequence))
  (def oscillating-sequence (generate-oscillating-sequence))


  (require '[clojure.data.csv :as csv]
           '[clojure.java.io :as io])


  (let [length 128]

    (with-open [sine-writer (io/writer "out.sine.csv")
                polynomial-writer (io/writer "out.polynomial.csv")
                polynomial-prime-writer (io/writer "out.polynomial.prime.csv")
                sine+cosine-writer (io/writer "out.sine+cosine.csv")
                oscillating-writer (io/writer "out.oscillating.csv")]

      ;; Sine
      (let [xaxis (range)
            yaxis (generate-sine-sequence)]

        (->> (interleave xaxis yaxis)
             (partition 2)
             (take length)
             (csv/write-csv sine-writer)))

      ;; Polynomial
      (let [xaxis (range)
            yaxis (->> (range)
                       (map polynomial-redux) ;; polynomial-xintercept ;; generate-polynomial-sequence
                       )]

        (->> (interleave xaxis yaxis)
             (partition 2)
             (take length)
             (csv/write-csv polynomial-writer)))

      #_(let [xaxis (->> (iterate #(+ % 1/10) 0)
                       (map float))
            yaxis (->> (iterate #(+ % 1/10) 0)
                       (map #(/ % 10))
                       (map polynomial-redux) ;; polynomial-xintercept ;; generate-polynomial-sequence
                       (take length))]

        (->> (interleave xaxis yaxis)
             (partition 2)
             (take length)
             (csv/write-csv polynomial-writer)))

      ;; sin t + cos (sqrt(3)t)
      (let [xaxis (range -10 10)
            yaxis (->> (range -10 10)
                       (map sine+cosine))]
        (->> (interleave xaxis yaxis)
             (partition 2)
             (take length)
             (csv/write-csv sine+cosine-writer)))

      ;; Oscillating
      (let [xaxis (range)
            yaxis (generate-oscillating-sequence)]

        (->> (interleave xaxis yaxis)
             (partition 2)
             (take length)
             (csv/write-csv oscillating-writer))))))

;; Traversing Data
;; loop / recur (find-xintercept)

;; Branching and Conditional Dispatch
;;   case , cond , multi methods , pattern matching (core.match)

;; First Order Functions. http://christophermaier.name/blog/2011/07/07/writing-elegant-clojure-code-using-higher-order-functions
;; partial, apply, comp, juxt

;; [x] repeatedly -> reduce (not lazy)
;; [ok] repeatedly -> iterate (lazy)
;; Transducers ...

;; sorting, grouping, filtering, dequeing
;; Scalars: numbers & precision




;; >> ================================= >>

(def beta-configurations
  "Get an intuition for the probability curves, generated by these alpha / beta curves
   https://en.wikipedia.org/wiki/Beta_distribution"

  {:bigswings {:alpha 0.5 :beta 0.5}
   :midpoint {:alpha 2.0 :beta 2.0}

   :left-leaning {:alpha 2.0 :beta 5.0}

   :highend {:alpha 3.0 :beta 1.0}
   :lowend {:alpha 1.0 :beta 3.0}

   :alternating-price-changes {:alpha 3.0 :beta 2.65}})

(defn ->beta-distribution [alpha beta]
  (BetaDistribution. alpha beta))

(defn price-swing-occurence-sequence [chunk-multiple beta-distribution]
  (->> (repeatedly #(.sample beta-distribution))
       (map #(* % chunk-multiple))
       (map #(Math/round %))))

(defn price-change-sequence [beta-distribution]
  (->> (repeatedly #(.sample beta-distribution))
       (map #(if (> % 0.5) + -))))

(defn generate-price-directions [length-of-price-direction price-change-inputs]
  (->> (price-change-sequence (apply ->beta-distribution price-change-inputs))
       (take length-of-price-direction)))


(defn generate-price-changes
  "0 - 0.3     / high
   0.31 - 0.45 / low
   0.46 - 0.6  / midpoint
   0.61 - 1    / bigswings"

  [beta-left-leaning
   beta-highend beta-lowend beta-midpoint beta-bigswings]

  (->> (repeatedly #(.sample beta-left-leaning))
       (map #(cond
               (< % 0.31) (.sample beta-highend)
               (< % 0.46) (.sample beta-lowend)
               (< % 0.61) (.sample beta-midpoint)
               :else (.sample beta-bigswings)))))

(defn generate-prices [beta-configurations]

  (let [;; BETA Distributions

        ;; > BIG SWINGS + MIDPOINT
        beta-bigswings
        (->> beta-configurations
             :bigswings vals
             (apply ->beta-distribution))

        beta-midpoint
        (->> beta-configurations
             :midpoint vals
             (apply ->beta-distribution))


        ;; > LEFT LEANING
        beta-left-leaning
        (->> beta-configurations
             :left-leaning vals
             (apply ->beta-distribution))


        ;; > HIGH END + LOW END
        beta-highend
        (->> beta-configurations
             :highend vals
             (apply ->beta-distribution))

        beta-lowend
        (->> beta-configurations
             :lowend vals
             (apply ->beta-distribution))


        alternating-price-change-inputs
        (->> beta-configurations
             :alternating-price-changes vals
             (iterate reverse))

        chunk-multiple 50
        price-swings (price-swing-occurence-sequence chunk-multiple beta-midpoint)

        price-directions (->> (map generate-price-directions
                                   price-swings
                                   alternating-price-change-inputs)
                              (apply concat))

        price-changes (generate-price-changes beta-left-leaning
                                              beta-highend beta-lowend
                                              beta-midpoint beta-bigswings)

        price-change-partials (map (fn [price-direction price-change]
                                     #(price-direction % price-change))
                                   price-directions
                                   price-changes)

        initial-price (rand-double-in-range 15 35)]



    (let [xaxis (range)
          yaxis (reductions (fn [acc price-change-partial]
                              (price-change-partial acc))
                            initial-price
                            price-change-partials)]

      (->> (interleave xaxis yaxis)
           (partition 2)))))

(comment ;; FINAL

  (require '[clojure.data.csv :as csv]
           '[clojure.java.io :as io])

  (with-open [writer (io/writer "out.final.csv")]

    (let [length 500]

      (->> (generate-prices beta-configurations)
           (take length)
           (csv/write-csv writer)))))

(comment

  (->> beta-configurations
       :bigswings
       vals
       (apply ->beta-distribution))


  ;; > C. PRICE SWINGS
  (def beta-midpoint
    (->> beta-configurations
         :midpoint
         vals
         (apply ->beta-distribution)))

  (def price-swings (price-swing-occurence-sequence 50 beta-midpoint))


  ;; A. PRICE CHANGE DIRECTION
  ;; (def beta-highend
  ;;   (->beta-distribution 3.0 2.5)
  ;;   #_(->> beta-configurations
  ;;        :highend
  ;;        vals
  ;;        (apply ->beta-distribution)))
  ;; (def price-changes (price-change-sequence (->beta-distribution 3.0 2.75) #_beta-highend))

  (->> (price-change-sequence (->beta-distribution 3.0 2.65))
       (take 100)
       (filter #(= - %))
       count)

  )

(comment

  ;; Greater price swings at opposite ends
  ;; alpha = beta = 0.5

  ;; Prices within 0.25 / 0.75
  ;; alpha = beta = 2.0

  ;; Prices closer to the high end
  ;; alpha = 5 / beta = 1

  ;; Prices closer to the low end
  ;; alpha = 1 / beta = 3
  ;; alpha = 1 / beta = 5

  ;; https://en.wikipedia.org/wiki/Beta_distribution

  (def beta-distribution (BetaDistribution. 2.0 2.0))
  (def betas (repeatedly #(.sample beta-distribution)))

  (require '[clojure.data.csv :as csv]
           '[clojure.java.io :as io])

  (with-open [writer-highend (io/writer "out.beta-highend.csv")
              writer-lowend (io/writer "out.beta-lowend.csv")]
    (let [length 128
          xaxis (range)

          beta-midpoint (BetaDistribution. 2.0 2.0)
          yaxis (repeatedly #(.sample beta-midpoint))

          beta-bigswings (BetaDistribution. 0.5 0.5)
          ybigswings (repeatedly #(.sample beta-bigswings))

          beta-highend (BetaDistribution. 3 1)
          yhighend (repeatedly #(.sample beta-highend))

          beta-lowend (BetaDistribution. 1 3)
          ylowend (repeatedly #(.sample beta-lowend))]

      (->> (interleave xaxis yaxis)
           (partition 2)
           (take length)
           (csv/write-csv writer-midpoint))

      (->> (interleave xaxis ybigswings)
           (partition 2)
           (take length)
           (csv/write-csv writer-bigswings))

      (->> (interleave xaxis yhighend)
           (partition 2)
           (take length)
           (csv/write-csv writer-highend))

      (->> (interleave xaxis ylowend)
           (partition 2)
           (take length)
           (csv/write-csv writer-lowend))
      )))
