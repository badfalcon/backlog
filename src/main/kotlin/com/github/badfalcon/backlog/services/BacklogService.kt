package com.github.badfalcon.backlog.services;

import com.github.badfalcon.backlog.config.MyPluginSettingsState
import com.intellij.openapi.components.Service;
import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.BacklogClientFactory
import com.nulabinc.backlog4j.conf.BacklogConfigure
import com.nulabinc.backlog4j.conf.BacklogJpConfigure

@Service(Service.Level.APP)
public class BacklogService {
    var backlogClient : BacklogClient? = null
    var isReady : Boolean = backlogClient != null

    init {
        val settings: MyPluginSettingsState = MyPluginSettingsState.getInstance()

        if(settings.apiKey != "" && settings.workspaceName != ""){
            val configure: BacklogConfigure = BacklogJpConfigure(settings.workspaceName).apiKey(settings.apiKey);
            if(isValidBacklogConfigs(settings.workspaceName, settings.apiKey)){
                backlogClient = BacklogClientFactory(configure).newClient()
            }
        }
    }

    /**
     * Checks if the passed values are valid for backlog
     */
    fun isValidBacklogConfigs(workspaceName: String, apiKey: String): Boolean {
        if(workspaceName == "" || apiKey == ""){
            return false
        }
        val configure: BacklogConfigure = BacklogJpConfigure(workspaceName).apiKey(apiKey)
        val newClient: BacklogClient = BacklogClientFactory(configure).newClient()
        if(newClient.myself.name != null){
            backlogClient = newClient
//            requestToolWindowUpdate()
            return true
        }
        return false
    }
}
