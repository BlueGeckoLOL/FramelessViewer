package me.bluegecko

import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.system.exitProcess

class App : JFrame() {
    companion object {
        var appData = AppData(
            true,
            Rectangle(0, 0, 600, 400),
            "",
            0,
            0,
            mutableListOf(""),
            0,
            Regex(".jpg|.jpeg|.png|.webp")
        )
    }
    private var iconLabel: JLabel

    init {
        title = "FramelessViewer"
        defaultCloseOperation = EXIT_ON_CLOSE
        isUndecorated = appData.isUndecorated
        bounds = appData.bounds

        appData.frameWidth = width - insets.left - insets.right
        appData.frameHeight = height - insets.top - insets.bottom

        val popupMenu = PopupMenu(this)
        val itemTitleBar = JMenuItem("Toggle TitleBar")
        val itemOpen = JMenuItem("Open")
        val itemExit = JMenuItem("Exit")
        itemTitleBar.addActionListener { toggleTitleBar() }
        itemOpen.addActionListener { open() }
        itemExit.addActionListener{ exit() }
        popupMenu.add(itemTitleBar)
        popupMenu.add(itemOpen)
        popupMenu.add(itemExit)

        iconLabel = JLabel()
        iconLabel.horizontalAlignment = JLabel.CENTER
        iconLabel.verticalAlignment = JLabel.CENTER

        val panel = JPanel()
        panel.layout = GridBagLayout()
        panel.add(iconLabel)

        contentPane.add(panel, BorderLayout.CENTER)

        addComponentListener(WindowResizeListener())
        addKeyListener(ArrowKeyListener())
        transferHandler = DropFileHandler()
    }

    inner class WindowResizeListener : ComponentListener {
        override fun componentHidden(p0: ComponentEvent?) {}
        override fun componentMoved(p0: ComponentEvent?) {}
        override fun componentShown(p0: ComponentEvent?) {}

        override fun componentResized(p0: ComponentEvent?) {
            appData.frameWidth = size.width - insets.left - insets.right
            appData.frameHeight = size.height - insets.top - insets.bottom

            this@App.updateImage()
        }
    }

    inner class ArrowKeyListener : KeyListener {
        override fun keyPressed(p0: KeyEvent?) {}
        override fun keyTyped(p0: KeyEvent?) {}
        override fun keyReleased(p0: KeyEvent?) {
            if (p0 != null && appData.filePath != "") {
                if (p0.keyCode == 37 || p0.keyCode == 39) { // Left arrow key: 37 | Right arrow key: 39
                    updateFileList()
                    if (p0.keyCode == 37) { // Left arrow key
                        if (appData.fileListIndex - 1 < 0) {
                            appData.fileListIndex = appData.fileList.size - 1
                        } else {
                            appData.fileListIndex -= 1
                        }
                        appData.filePath = appData.fileList[appData.fileListIndex]
                        updateImage()
                    } else if (p0.keyCode == 39) { // Right arrow key
                        if (appData.fileListIndex + 1 >= appData.fileList.size) {
                            appData.fileListIndex = 0
                        } else {
                            appData.fileListIndex += 1
                        }
                        appData.filePath = appData.fileList[appData.fileListIndex]
                        updateImage()
                    }
                }
            }
        }
    }

    inner class DropFileHandler : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDrop) {
                return false
            }
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false
            }
            return true
        }
        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }
            val t = support.transferable
            try {
                val files = t.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                val filePath = files[0].toString()
                if (!filePath.contains(appData.extensionRegex)) {
                    return false
                }
                appData.filePath = filePath
                updateImage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }
    }

    private fun toggleTitleBar() {
        appData.isUndecorated = !appData.isUndecorated
        appData.bounds = bounds
        this@App.isVisible = false
    }

    private fun open() {
        val chooser = JFileChooser()
        chooser.showOpenDialog(null)
        val file = chooser.selectedFile
        if (file != null) {
            appData.filePath = file.absolutePath
            updateImage()
            updateFileList()
        }
    }

    private fun exit() {
        exitProcess(0)
    }

    private fun updateImage() {
        try {
            val file = File(appData.filePath)
            val bufferedImage = ImageIO.read(file)

            var width = this@App.width
            var height = this@App.height
            if (!appData.isUndecorated) {
                width = appData.frameWidth
                height = appData.frameHeight
            }

            if (bufferedImage.width > width || bufferedImage.height > height) {
                val widthStandardSize = Pair(width, getSizeFromAspectRatio(width, getAspectRatio(bufferedImage.width, bufferedImage.height), false))
                val heightStandardSize = Pair(getSizeFromAspectRatio(height, getAspectRatio(bufferedImage.width, bufferedImage.height), true), height)

                if (width >= widthStandardSize.first && height >= widthStandardSize.second) {
                    val image = bufferedImage.getScaledInstance(widthStandardSize.first, widthStandardSize.second, Image.SCALE_SMOOTH)
                    iconLabel.icon = ImageIcon(image)
                } else if (width >= heightStandardSize.first && height >= heightStandardSize.second) {
                    val image = bufferedImage.getScaledInstance(heightStandardSize.first, heightStandardSize.second, Image.SCALE_SMOOTH)
                    iconLabel.icon = ImageIcon(image)
                }
            } else {
                iconLabel.icon = ImageIcon(bufferedImage)
            }

            title = "${File(appData.filePath).name} [${appData.fileListIndex+1}/${appData.fileList.size}] | FramelessViewer"
        } catch (e: IIOException) {
            iconLabel.text = ""
        }
    }

    private fun updateFileList() {
        val filePathSlashIndex = appData.filePath.lastIndexOf("/")
        val dirPath = appData.filePath.substring(0, filePathSlashIndex)
        val dir = File(dirPath)
        val rawFileList = dir.listFiles()
        val filenameList = rawFileList?.filter { it.isFile }?.map { it.absolutePath.toString() }?.filter { it.contains(
            appData.extensionRegex) }
            ?.sorted()
        appData.fileList = filenameList as MutableList<String>
        appData.fileListIndex = appData.fileList.indexOf(appData.filePath)
        title = "${File(appData.filePath).name} [${appData.fileListIndex+1}/${appData.fileList.size}] | FramelessViewer"
    }

    // Example: firstI1: 1920, firstI2: 1080 = Pair<16, 9>
    private fun getAspectRatio(firstI1: Int, firstI2: Int): Pair<Int, Int> {
        var i1 = firstI1
        var i2 = firstI2
        while (true) {
            val ans = i1 % i2
            if (ans == 0) {
                return firstI1 / i2 to firstI2 / i2
            } else {
                i1 = i2
                i2 = ans
                continue
            }
        }
    }

    // Example: size: 1920, i: Pair<16, 9>, isHeightStandard = true/false = 1080
    private fun getSizeFromAspectRatio(size: Int, i: Pair<Int, Int>, isHeightStandard: Boolean): Int {
        if (isHeightStandard) {
            return (size * i.first) / i.second
        }
        return (size * i.second) / i.first
    }
}