package de.ml.backupfromchangelist.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.gridLayout.GridLayout
import javax.swing.*
import java.awt.BorderLayout

class BackupServerConfigurable : Configurable {

    private var panel: JPanel? = null
    private val listModel = DefaultListModel<String>()
    private val list = JBList(listModel)

    private val nameField = JTextField()
    private val hostField = JTextField()
    private val portField = JSpinner(SpinnerNumberModel(22, 1, 65535, 1))
    private val userField = JTextField()
    private val passwordField = JPasswordField()
    private val keyPathField = JTextField()
    private val authMethodBox = ComboBox(arrayOf("password", "key"))

    private var currentServer: BackupServer? = null

    override fun getDisplayName(): String = "Changelist Backup"

    override fun createComponent(): JComponent {
        if (panel == null) {
            panel = JPanel(BorderLayout())

            // Liste links
            list.selectionMode = ListSelectionModel.SINGLE_SELECTION
            list.addListSelectionListener {
                //saveCurrentServer()
                //val server = list.selectedValue
                //if (server != null) {
                //    loadServer(server)
                //}
            }

            val listPanel = JPanel(BorderLayout())
            listPanel.add(JScrollPane(list), BorderLayout.CENTER)

            val addButton = JButton("Add").apply {
                addActionListener {
                    val newServer = BackupServer(name = "New Server")
                    listModel.addElement(newServer.name)
                    val idx = listModel.size - 1
                    list.selectedIndex = idx
                    list.ensureIndexIsVisible(idx)
                }
            }

            val removeButton = JButton("Remove").apply {
                addActionListener {
                    val idx = list.selectedIndex
                    if (idx >= 0) {
                        listModel.remove(idx)
                        currentServer = null
                        clearFields()
                    }
                }
            }

            val buttonPanel = JPanel().apply {
                add(addButton)
                add(removeButton)
            }
            listPanel.add(buttonPanel, BorderLayout.SOUTH)

            // Formular rechts
            val formPanel = JPanel(BorderLayout())
            formPanel.add(JLabel("Name:")); formPanel.add(nameField)
            formPanel.add(JLabel("Host:")); formPanel.add(hostField)
            formPanel.add(JLabel("Port:")); formPanel.add(portField)
            formPanel.add(JLabel("User:")); formPanel.add(userField)
            formPanel.add(JLabel("Auth:")); formPanel.add(authMethodBox)
            formPanel.add(JLabel("Password:")); formPanel.add(passwordField)
            formPanel.add(JLabel("Private Key:")); formPanel.add(keyPathField)

            val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, formPanel)
            split.dividerLocation = 200
            panel!!.add(split, BorderLayout.CENTER)
        }
        return panel!!
    }

    override fun isModified(): Boolean = true

    override fun apply() {
        val servers = listModel.elements().toList().map { BackupServer(name = it) }
        // Speichern im Settings-Objekt
        val settings = BackupServerSettings.getInstance(null!!) // Achtung: project=null vermeiden
        settings.servers = servers.toMutableList()
    }

    private fun loadServer(server: BackupServer) {
        currentServer = server
        nameField.text = server.name
        hostField.text = server.host
        portField.value = server.port
        userField.text = server.user
        authMethodBox.selectedItem = server.authMethod
        passwordField.setText(server.password ?: "")
        keyPathField.text = server.privateKeyPath ?: ""
    }

    private fun saveCurrentServer() {
        val server = currentServer ?: return
        server.name = nameField.text
        server.host = hostField.text
        server.port = (portField.value as Int)
        server.user = userField.text
        server.authMethod = authMethodBox.selectedItem as String
        server.password = String(passwordField.password)
        server.privateKeyPath = keyPathField.text
    }

    private fun clearFields() {
        nameField.text = ""
        hostField.text = ""
        portField.value = 22
        userField.text = ""
        passwordField.setText("")
        keyPathField.text = ""
    }
}