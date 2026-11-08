package de.ml.backupfromchangelist.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
@State(
    name = "ChangelistBackupServers",
    storages = [Storage("changelistBackup.xml")]
)
class BackupServerSettings : PersistentStateComponent<BackupServerSettings> {

    var servers: MutableList<BackupServer> = mutableListOf()

    override fun getState(): BackupServerSettings = this

    override fun loadState(state: BackupServerSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): BackupServerSettings =
            project.getService(BackupServerSettings::class.java)
    }
}