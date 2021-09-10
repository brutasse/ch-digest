(ns clubhouse.views
  (:require
    [re-frame.core :as re-frame]
    [clubhouse.events :as events]
    [clubhouse.subs :as subs]
    [soda-ash.core :as sa]
    [reagent.core :as reagent]
    [cljsjs.moment]
    [markdown.core :refer [md->html]]))

(defn header-menu
  []
  (let [{:keys [day next? previous?]} @(re-frame/subscribe [::subs/day])]
    [sa/Menu {:inverted true}
     [sa/Container
      [sa/MenuItem "Shortcut Conversations"]
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

(defn project-section
  [project stories metadata]
  [:<>
   [sa/Header {:as "h2"} (:name project)]
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
