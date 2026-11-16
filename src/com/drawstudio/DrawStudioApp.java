package com.drawstudio;

import javax.swing.*;
import java.awt.*;

/** 澔鼠の奇妙喵屋程序入口。 */
public final class DrawStudioApp {

    private DrawStudioApp() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 主题类中还会尝试使用 Nimbus。
        }
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.install();
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Throwable ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "澔鼠の奇妙喵屋启动失败：\n" + ex.getMessage(),
                        "启动错误", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
