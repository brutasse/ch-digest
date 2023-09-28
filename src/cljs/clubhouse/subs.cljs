(ns clubhouse.subs
  (:require [re-frame.core :as re-frame]))

(defmulti handle-action
  (fn [_ _ _ {:keys [entity_type action]}]
    (keyword (str entity_type "-" action))))

(defn event->workflow-states
  [{:keys [references]}]
  (into {} (for [reference references
                 :when (= (:entity_type reference) "workflow-state")]
             [(:id reference) reference])))

(defmethod handle-action :story-create
  [db event data action]
  (let [id (:id action)
        full-story (get-in db [:stories (-> id str keyword)])
        story {:id id
               :state (:workflow_state_id action)}]
    (-> data
      (assoc-in [:projects (:project_id full-story) id] story)
      (update :workflow-states merge (event->workflow-states event)))))

(defmethod handle-action :story-update
  [db event data {:keys [changes] :as action}]
  (let [id                 (:id action)
        full-story         (get-in db [:stories (-> id str keyword)])
        {:keys [state
                old-state
                comments]} (get-in data [:projects (:project_id full-story) id])
        story (cond->
                {:id id
                 :state (:workflow_state_id full-story)
                 :comments comments}

                (:workflow_state_id changes)
                (assoc :old-state (-> changes :workflow_state_id :old)
                       :state (-> changes :workflow_state_id :new))

                old-state
                (assoc :old-state old-state)

                (:comment_ids changes)
                (assoc :comments (concat comments
                                         (-> changes :comment_ids :adds))))]

    (when (not (or
                 ; handled ones
                 (:workflow_state_id changes)
                 (:comment_ids changes)
                 ; unhandled
                 (:name changes)
                 (:follower_ids changes)
                 (:epic_id changes)
                 (:owner_ids changes)
                 (:blocked changes)
                 (:description changes)
                 (:label_ids changes)
                 (:branch_ids changes)
                 (:archived changes)
                 (:task_ids changes)
                 (:story_type changes)
                 (:iteration_id changes)
                 (:file_ids changes)
                 (:object_story_link_ids changes)
                 (:estimate changes)
                 (:subject_story_link_ids changes)
                 (:deadline changes)
                 (:commit_ids changes)
                 (:requested_by_id changes)
                 (:blocker changes)
                 (:pull_request_ids changes)
                 (:position changes)
                 (:external_links changes)
                 (:group_id changes)
                 (:custom_field_value_ids changes)
                 (:project_id changes)))
      (println "UNHANDLED CHANGE" (pr-str changes)))

    (-> data
      (assoc-in [:projects (:project_id full-story) id] story)
      (update :workflow-states merge (event->workflow-states event)))))

(defmethod handle-action :story-delete
  [db event data action]
  data)

(defmethod handle-action :story-comment-create
  [db event data action]
  (assoc-in data [:comments (:id action)]
            (assoc action :created (:changed_at event))))

(defmethod handle-action :story-comment-delete
  [db event data action]
  data)

(defmethod handle-action :story-comment-update
  [db event data action]
  data)

(defmethod handle-action :story-task-create
  [db event data action]
  data)

(defmethod handle-action :story-task-update
  [db event data action]
  data)

(defmethod handle-action :story-task-delete
  [db event data action]
  data)

(defmethod handle-action :branch-create
  [db event data action]
  data)

(defmethod handle-action :branch-merge
  [db event data action]
  data)

(defmethod handle-action :pull-request-create
  [db event data action]
  data)

(defmethod handle-action :pull-request-update
  [db event data action]
  data)

(defmethod handle-action :pull-request-comment
  [db event data action]
  data)

(defmethod handle-action :pull-request-close
  [db event data action]
  data)

(defmethod handle-action :epic-update
  [db event data action]
  data)

(defmethod handle-action :label-create
  [db event data action]
  data)

(defmethod handle-action :story-link-create
  [db event data action]
  data)

(defmethod handle-action :story-link-delete
  [db event data action]
  data)

(defmethod handle-action :epic-comment-create
  [db event data action]
  data)

(defmethod handle-action :epic-create
  [db event data action]
  data)

(defmethod handle-action :epic-delete
  [db event data action]
  data)

(defmethod handle-action :branch-push
  [db event data action]
  data)

(defmethod handle-action :pull-request-open
  [db event data action]
  data)

(defmethod handle-action :reaction-create
  [db event data action]
  data)

(defmethod handle-action :default
  [db event data action]
  (println "Unknown action" (pr-str action) (pr-str event))
  data)

(defn handle-event
  [db acc {:keys [actions] :as event}]
  (reduce (partial handle-action db event) acc actions))

(re-frame/reg-sub
  ::meta
  (fn [db]
    (select-keys db [:members :projects :stories])))

(re-frame/reg-sub
  ::state
  (fn [{:keys [events] :as db}]
    (reduce (partial handle-event db) {} events)))

(re-frame/reg-sub
  ::day
  (fn [{:keys [day min-day max-day]}]
    {:day day
     :next? (> max-day day)
     :previous? (> day min-day)}))

(re-frame/reg-sub
  ::pulling?
  (fn [db]
    (:pulling? db)))
