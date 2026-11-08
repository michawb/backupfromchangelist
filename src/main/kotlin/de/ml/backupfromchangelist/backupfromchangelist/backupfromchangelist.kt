package de.ml.backupfromchangelist.backupfromchangelist

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.remoteServer.ServerType
import com.intellij.remoteServer.configuration.RemoteServersManager
import com.jetbrains.plugins.webDeployment.config.GroupedServersConfigManager
import de.ml.backupfromchangelist.settings.BackupServerSettings


import java.io.File






class ExportChangelistPopupGroup : ActionGroup() {


    /**
     *
     */
    fun listAvailableServerTypes(project : Project) {

        for (type in ServerType.EP_NAME.extensions) {
            println("Available ServerType: id='${type.id}', class='${type.javaClass.name}', presentableName='${type.presentableName}'")
        }

        val remoteServers = GroupedServersConfigManager.getInstance(project).flattenedServers
        println("New Remote-Servers")
        remoteServers.forEach{server ->
            println(server)
        }
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return emptyArray()
        listAvailableServerTypes(project)

        //val deploymentManager = project.getService(WebServers::class.java)
        //val list = DeploymentConfigurationManager.getInstance(project).getDeploymentConfigurations().javaClass.methods

        //for (server in list) {
            //println("Deployment Server: ${server.name}")
            //println("Host: ${server.host}")
            //println("Port: ${server.port}")
            //println("Remote root: ${server.remoteRoot}")
        //}


        //for (config in list) {
        //    println("Deployment config: ${config.name}, type: ${config.type}, class: ${config::class.qualifiedName}")
        //}
        //for (cfg in list){
            //println( "HOST: ${cfg}" )
        //}

        // Alle definierten Deployment-Server abrufen
        val servers = RemoteServersManager.getInstance().servers

        if (servers.isEmpty()) {
            return arrayOf(
                object : AnAction("Keine Deployment-Server definiert") {
                    override fun actionPerformed(e: AnActionEvent) {
                        println("Server sind leer")
                        //println(list)
                    }
                }
            )
        }

        val servers2 = BackupServerSettings.getInstance(project).servers
        servers2.forEach {
            println("Backupserver: ${it.name} @ ${it.host}:${it.port}")
        }

        // Für jeden Server eine Action erzeugen
        return servers.map { server ->
            object : AnAction("Backup nach ") {
                override fun actionPerformed(e: AnActionEvent) {

                    //performBackup(project, server)
                }
            }
        }.toTypedArray()
    }

    /**
     *  Lade Dateiliste aus Changelist und speichern
     */
    private fun performBackup(project: Project, server: RemoteServersManager) {
        val clm = ChangeListManager.getInstance(project)
        val changelist = clm.defaultChangeList
        val files = changelist.changes.mapNotNull { it.virtualFile }

        // Lokale Export-Datei
        val exportFile = File("${project.basePath}/changelist.txt")
        exportFile.printWriter().use { out ->
            files.forEach { out.println(it.path) }
        }

        // Hier würdest du den Upload starten
        // Entweder mit Deployment API, oder simpler: eigenes Script mit Server-Infos
        val server1 = server
        println("Backup nach $server1 vorbereitet.")
    }




}
