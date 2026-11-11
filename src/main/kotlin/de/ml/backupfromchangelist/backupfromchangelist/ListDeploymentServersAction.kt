package de.ml.backupfromchangelist

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.ssh.config.unified.SshConfigManager
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SelectDeploymentServerAction : AnAction("Backup aus Changelist erstellen") {

    // ---- Datenmodell für Popup ----
    data class DeploymentServerInfo(
        val id: String,
        val displayName: String,
        val host: String,
        val port: Int,
        val user: String?,
        val type: String,
        val pw: @NlsSafe String?,
        val rootFolder: String? = "/",
        val group: Any? = ""
    )

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val servers = loadDeploymentServers(project)
        if (servers.isEmpty()) {
            Messages.showInfoMessage(project, "No Deployment-Servers found.", "Changelist Backup")
            return
        }

        showServerPopup(project, e.dataContext, servers) { selected ->
            // Hier deine Backup-Logik aufrufen
            println("Gewählt: ${selected.displayName}")
            doBackup(project, selected)
        }

    }

    private fun doBackup(project: Project, chosen: DeploymentServerInfo){

        val task = object : Task.Backgroundable(project, "Backup changelist run...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false

                val clm = ChangeListManager.getInstance(project)
                val files = clm.defaultChangeList.changes.mapNotNull { it.virtualFile }
                val total = files.size
                var count = 0
                val jsch = JSch()
                val session: Session = jsch.getSession(chosen.user, chosen.host, chosen.port)

                session.setPassword(chosen.pw)
                session.setConfig("StrictHostKeyChecking", "no")
                session.connect()

                try {
                    // Backup-Root vorbereiten
                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    val timestamp = LocalDateTime.now().format(formatter)
                    val backupRoot = "${chosen.rootFolder}/.backup/$timestamp"
                    executeSshCommand(session, "mkdir -p $backupRoot")

                    // Dateien nacheinander sichern
                    files.forEach { file ->
                        count++
                        indicator.text = "backup file: ${file.name}"
                        indicator.fraction = count.toDouble() / total.toDouble()

                        val relativePath = file.path.removePrefix(project.basePath ?: "").trimStart('/')
                        val remoteFilePath = "${chosen.rootFolder}/$relativePath"
                        val backupFilePath = "$backupRoot/$relativePath"

                        val parentDir = backupFilePath.substringBeforeLast("/", "")
                        if (parentDir.isNotEmpty()) {
                            executeSshCommand(session, "mkdir -p $parentDir")
                        }

                        executeSshCommand(session, "cp $remoteFilePath $backupFilePath")
                    }
                } finally {
                    session.disconnect()
                }
            }

            override fun onFinished() {
                Messages.showInfoMessage(project, "Backup successful on ${chosen.displayName}", "Changelist Backup")
            }
        }

        ProgressManager.getInstance().run(task)
    }

    private fun showServerPopup(project: Project, dataContext: DataContext, servers: List<DeploymentServerInfo>, onChosen: (DeploymentServerInfo) -> Unit) {
        val step = object : BaseListPopupStep<DeploymentServerInfo>("Choose Server to Sync With", servers) {

            override fun getTextFor(value: DeploymentServerInfo): String = value.displayName
            //override fun getIconFor(value: DeploymentServerInfo): Icon? = value.icon

            override fun hasSubstep(selectedValue: DeploymentServerInfo?): Boolean = false

            override fun onChosen(selectedValue: DeploymentServerInfo, finalChoice: Boolean): PopupStep<*>? {
                onChosen(selectedValue)
                return PopupStep.FINAL_CHOICE
            }
        }

        val popup: ListPopup = JBPopupFactory.getInstance().createListPopup(step)
        popup.showInBestPositionFor(dataContext)
    }

    // ---- Loader über Reflection ----
    private fun loadDeploymentServers(project: Project): List<DeploymentServerInfo> {
        val result = mutableListOf<DeploymentServerInfo>()

        try {
            val managerClass = Class.forName("com.jetbrains.plugins.webDeployment.config.GroupedServersConfigManager")
            val getInstance = managerClass.getMethod("getInstance", Project::class.java)
            val instance = getInstance.invoke(null, project)

            val getServers = managerClass.getMethod("getFlattenedServers")
            val servers = getServers.invoke(instance) as Collection<*>

            val serverClass = Class.forName("com.jetbrains.plugins.webDeployment.config.WebServerConfig")

            servers.forEach { s ->
                val server = serverClass.cast(s)
                val id = serverClass.getMethod("getId").invoke(server).toString()
                val name = serverClass.getMethod("getName").invoke(server).toString()
                val rootUri = serverClass.getMethod("getRootUri").invoke(server).toString()

                val fileTransferConfig = serverClass.getMethod("getFileTransferConfig").invoke(server)

                val host: String
                val user: String?
                val port: Int?
                val root = fileTransferConfig.javaClass.getMethod("getRootFolder").invoke(fileTransferConfig).toString()

                if(serverClass.getMethod("isProjectLevel").invoke(server) == true){

                    when {
                        rootUri.startsWith("sftp:") -> {
                            val sshId = fileTransferConfig
                                .javaClass.getMethod("getSshPresentableId")
                                .invoke(fileTransferConfig)
                                .javaClass.getMethod("getId")
                                .invoke(fileTransferConfig.javaClass.getMethod("getSshPresentableId").invoke(fileTransferConfig))

                            val sshConfig = SshConfigManager.getInstance(project).configs.find { it.id == sshId }
                            host = sshConfig?.host ?: "?"
                            user = sshConfig?.username
                            port = (sshConfig?.port ?: 21)

                            //val root = fileTransferConfig.javaClass.getMethod("getRootFolder").invoke(fileTransferConfig).toString()
                            val credentials = getCredentials("SshConfigPassword", sshConfig?.host + ":" + sshConfig?.port + " " + sshConfig?.id)
                            val password = credentials?.getPasswordAsString()

                            result.add(DeploymentServerInfo(id, name, host, port, user, "SFTP", password, root))
                        }
                        rootUri.startsWith("ftp:") -> {
                            host = fileTransferConfig.javaClass.getMethod("getHost").invoke(fileTransferConfig).toString()
                            val port = fileTransferConfig.javaClass.getMethod("getPort").invoke(fileTransferConfig)
                            val credentials = getCredentials("Deployment", id)
                            user = credentials?.userName

                            result.add(DeploymentServerInfo(id, name, "$host:$port", port as Int, user, "FTP",credentials?.getPasswordAsString(),root))
                        }
                        else -> {
                            result.add(DeploymentServerInfo(id, name, "?", 21,null, "Unknown", "..."))
                        }
                    }

                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return result
    }

    // ---- Credential Helper ----
    private fun getCredentials(subsystem: String, key: String): Credentials? {
        val attributes = CredentialAttributes(generateServiceName(subsystem, key))
        return PasswordSafe.instance.get(attributes)
    }

    fun executeSshCommand(session: Session, command: String) {
        val channel = session.openChannel("exec")
        (channel as ChannelExec).setCommand(command)
        channel.connect()

        // Warten auf die Antwort
        while (!channel.isClosed) {
            Thread.sleep(100)
        }

        if (channel.exitStatus != 0) {
            println("Fehler beim Ausführen des Befehls: $command")
        }
        channel.disconnect()
    }

}
