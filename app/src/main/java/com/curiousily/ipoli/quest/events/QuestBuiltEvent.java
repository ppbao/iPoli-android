package com.curiousily.ipoli.quest.events;

import com.curiousily.ipoli.quest.Quest;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 8/19/15.
 */
public class QuestBuiltEvent {
    public Quest quest;

    public QuestBuiltEvent(Quest quest) {
        this.quest = quest;
    }
}
