(ns clubhouse.views
  (:require
    [re-frame.core :as re-frame]
    [clubhouse.events :as events]
    [clubhouse.subs :as subs]
    [soda-ash.core :as sa]
    [reagent.core :as reagent]
    [cljsjs.moment]
    [markdown.core :refer [md->html]]))

(def story-color
  {"chore"   "blue"
   "feature" "yellow"
   "bug"     "red"})

(defn header-menu
  []
  (let [{:keys [day next? previous?]} @(re-frame/subscribe [::subs/day])]
    [sa/Menu {:inverted true}
     [sa/Container
      [sa/MenuItem "ClubHouse Activity"]
      [sa/MenuItem
      [sa/ButtonGroup {:size :tiny}
      [sa/Button {:disabled (not previous?)
                  :size :tiny
                  :on-click #(re-frame/dispatch [::events/change-day -1])}
       "prev"]
      [sa/Button {:size :tiny :color :teal} (str day)]
      [sa/Button {:disabled (not next?)
                  :size :tiny
                  :on-click #(re-frame/dispatch [::events/change-day 1])}
       "next"]]]]]))


(def prev-col-counts
  {"Unscheduled" 0
   "Ready for Development" 0
   "In Development" 1
   "In progress" 1
   "Ready for Review" 2
   "Ready for Deploy" 3
   "Completed" 4})

(defn story-state
  [workflow-states [_ {:keys [old-state state]}]]
  (+
   (* 10 (prev-col-counts (:name (get workflow-states state))))
   (prev-col-counts (:name (get workflow-states (or old-state state))))))

(defn project-section
  [project stories metadata]
  [:<>
   [sa/Header {:as "h2"} (:name project)]
   [sa/Grid {:columns 5}
    [sa/GridRow {:stretched true}
     [sa/GridColumn [:strong "New"]]
     [sa/GridColumn [:strong "In Progress"]]
     [sa/GridColumn [:strong "Ready for review"]]
     [sa/GridColumn [:strong "Ready for deploy"]]
     [sa/GridColumn [:strong "Completed"]]]
    (for [[id story] (sort-by (partial story-state (:workflow-states metadata))
                              stories)
          :let [full-story     (get-in metadata
                                       [:stories (-> id str keyword)])
                workflow-state (get-in metadata
                                       [:workflow-states (:state story)])
                old-state      (get-in metadata
                                       [:workflow-states (:old-state story)])]]
      ^{:key id}
      [sa/GridRow {:stretched true}
       (if old-state
         (let [prev (get prev-col-counts (:name old-state))
               tot (get prev-col-counts (:name workflow-state))
               gap (- tot 1 prev)]
           [:<>
            (for [i (range prev)]
              ^{:key i} [sa/GridColumn])
            (when (not= prev tot)
              ^{:key "card"}
              [sa/GridColumn [sa/Card {:style {:opacity 0.4}
                                       :description (:name full-story)}]])
            (for [i (range gap)]
              ^{:key i} [sa/GridColumn {:verticalAlign :middle}
                         [:div {:style {:text-align :center}}
                     [sa/Icon {:name "right arrow"
                               :size :large
                               :style {:opacity 0.2}}]]])])
         (for [i (range (get prev-col-counts (:name workflow-state)))]
           ^{:key i} [sa/GridColumn ""]))
       [sa/GridColumn [sa/Card {:href (:app_url full-story)
                                :target :_blank
                                :color (story-color (:story_type full-story))
                                :description (:name full-story)}]]])]

   (for [[id story] stories
         :when (:comments story)
         :let [full-story (get-in metadata [:stories (-> id str keyword)])]]
     ^{:key id}
     [sa/CommentGroup
      [sa/Header {:as :h4
                  :dividing true}
       (:name full-story)
       [sa/Label {:as :a
                  :target :_blank
                  :href (:app_url full-story)}
        (str "[CH" (:id story) "]")]]

      (for [comm (:comments story)
            :let [full-comm (get-in metadata [:comments comm])
                  author    (get-in metadata [:members (-> full-comm
                                                         :author_id
                                                         keyword)])
                  gravatar  (str "https://www.gravatar.com/avatar/"
                                 (-> author :profile :gravatar_hash)
                                 ".jpg?s=48&d=identicon")
                  md        (-> full-comm :text md->html)]]
        ^{:key comm}
        [sa/CommentSA
         [sa/CommentAvatar {:src gravatar}]
         [sa/CommentContent
          [sa/CommentAuthor
           (-> author :profile :name)
           [sa/CommentMetadata {:style {:font-weight :normal}}
            (-> full-comm :created js/moment (.format "HH:mm")) ]]
          [sa/CommentText [:div {:dangerouslySetInnerHTML
                                 {:__html md}}]]]])])
   [sa/Divider]])

(defn project-name
  [{:keys [projects]} [project-id _]]
  (get-in projects [(-> project-id str keyword) :name]))

(defn projects-overview
  []
  (let [state    @(re-frame/subscribe [::subs/state])
        metadata (-> [::subs/meta]
                   re-frame/subscribe
                   deref
                   (assoc :workflow-states (:workflow-states state)
                          :comments (:comments state)))]
    [sa/Container
    (for [[id stories] (sort-by (partial project-name metadata)
                                (:projects state))
          :let [project (get-in metadata [:projects (-> id str keyword)])]
          :when project]
      ^{:key (str "project-" id)}
      [project-section project stories metadata])
     (when-not (:projects state)
       "No data.")]))

(defn main-panel []
  (let [pulling? @(re-frame/subscribe [::subs/pulling?])]
    [:<>
     [header-menu]
     (if pulling?
       [sa/Dimmer {:active true :inverted true}
        [sa/Loader {:inverted true} "Loading…"]]
       [projects-overview])]))
