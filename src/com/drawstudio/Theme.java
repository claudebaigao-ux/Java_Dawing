package com.drawstudio;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * 澔鼠の奇妙喵屋的视觉主题：颜色、字体以及一些轻量的自绘 Swing 组件。
 */
public final class Theme {

    public static final String APP_NAME = "澔鼠の奇妙喵屋";

    public static final Color WINDOW_BG = new Color(0xF2EFE8);
    public static final Color SURFACE = new Color(0xFFFFFF);
    public static final Color SURFACE_ALT = new Color(0xF8F5EE);
    public static final Color BORDER = new Color(0xE5DFD3);
    public static final Color INK = new Color(0x22302D);
    public static final Color INK_SOFT = new Color(0x68736F);
    public static final Color MUTED = new Color(0x8A938F);
    public static final Color ACCENT = new Color(0xE9AE45);
    public static final Color ACCENT_DARK = new Color(0xC98B22);
    public static final Color ACCENT_SOFT = new Color(0xF7D98F);
    public static final Color TEAL = new Color(0x2F6E64);
    public static final Color SIDEBAR = new Color(0x1E2B28);
    public static final Color SIDEBAR_HOVER = new Color(0x2A3A36);
    public static final Color SIDEBAR_TEXT = new Color(0xDCE5E0);
    public static final Color SIDEBAR_MUTED = new Color(0x8FA39C);
    public static final Color CANVAS_BG = new Color(0xE8E3D9);
    public static final Color DANGER = new Color(0xBE5D4D);

    public static final String FONT_FAMILY = chooseFontFamily();

    private Theme() {
    }

    private static String chooseFontFamily() {
        String[] preferred = {
                "Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC",
                "Noto Sans CJK SC", "Source Han Sans SC", "SimHei", "SansSerif"
        };
        try {
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            Set<String> names = new HashSet<String>();
            for (String name : available) {
                names.add(name);
            }
            for (String name : preferred) {
                if (names.contains(name)) {
                    return name;
                }
            }
        } catch (Throwable ignored) {
            // 在极少数无图形环境下仍然给出一个安全的回退字体。
        }
        return "SansSerif";
    }

    public static Font font(int size) {
        return new Font(FONT_FAMILY, Font.PLAIN, size);
    }

    public static Font font(int size, int style) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // 使用系统默认外观即可。
        }

        UIManager.put("control", WINDOW_BG);
        UIManager.put("info", SURFACE_ALT);
        UIManager.put("nimbusBase", TEAL);
        UIManager.put("nimbusBlueGrey", BORDER);
        UIManager.put("nimbusLightBackground", SURFACE);
        UIManager.put("nimbusSelectionBackground", ACCENT);
        UIManager.put("nimbusSelectedText", INK);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("text", INK);
        UIManager.put("ToolTip.background", INK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", font(12));
        UIManager.put("ToolTip.border", new EmptyBorder(6, 9, 6, 9));

        String[] fontKeys = {
                "Label.font", "Button.font", "ToggleButton.font", "CheckBox.font",
                "RadioButton.font", "ComboBox.font", "Spinner.font", "TextField.font",
                "TextArea.font", "TextPane.font", "Menu.font", "MenuItem.font",
                "CheckBoxMenuItem.font", "RadioButtonMenuItem.font", "PopupMenu.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "FileChooser.font",
                "TitledBorder.font", "List.font", "Table.font"
        };
        for (String key : fontKeys) {
            UIManager.put(key, font(13));
        }

        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && ((String) key).endsWith(".font")) {
                UIManager.put(key, font(13));
            }
        }
        UIManager.put("MenuBar.font", font(13, Font.BOLD));
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color mix(Color a, Color b, double amount) {
        amount = Math.max(0, Math.min(1, amount));
        int r = (int) Math.round(a.getRed() * (1 - amount) + b.getRed() * amount);
        int g = (int) Math.round(a.getGreen() * (1 - amount) + b.getGreen() * amount);
        int bl = (int) Math.round(a.getBlue() * (1 - amount) + b.getBlue() * amount);
        return new Color(r, g, bl);
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(14, Font.BOLD));
        label.setForeground(INK);
        return label;
    }

    public static JLabel muted(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(font(size));
        label.setForeground(MUTED);
        return label;
    }

    public static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** 带圆角填充、悬停反馈和可选图标的按钮。 */
    public static class RoundedButton extends JButton {
        private final Color fillColor;
        private final Color borderColor;
        private final int arc;

        public RoundedButton(String text, Icon icon, Color fill, Color foreground) {
            this(text, icon, fill, foreground, null, 11);
        }

        public RoundedButton(String text, Icon icon, Color fill, Color foreground,
                             Color border, int arc) {
            super(text);
            this.fillColor = fill;
            this.borderColor = border;
            this.arc = arc;
            setIcon(icon);
            setForeground(foreground);
            setFont(font(13, Font.BOLD));
            setIconTextGap(7);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(8, 13, 8, 13));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = fillColor;
            if (fill == null) {
                fill = alpha(Color.WHITE, 0);
            }
            if (!isEnabled()) {
                fill = mix(fill, SURFACE, 0.65);
            } else if (getModel().isPressed()) {
                fill = mix(fill, Color.BLACK, 0.10);
            } else if (getModel().isRollover()) {
                fill = mix(fill, Color.WHITE, 0.13);
            }
            if (fill.getAlpha() > 0) {
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
            }
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f,
                        getHeight() - 1f, arc, arc));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 左侧工具栏中的图标 + 文字卡片。 */
    public static class ToolButton extends JToggleButton {
        public ToolButton(String text, Icon icon) {
            super(text, icon);
            setFont(font(13));
            setForeground(SIDEBAR_TEXT);
            setHorizontalAlignment(LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setPreferredSize(new Dimension(138, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            getModel().addChangeListener(e -> refreshColor());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    repaint();
                }
            });
            refreshColor();
        }

        private void refreshColor() {
            setForeground(isSelected() ? INK : SIDEBAR_TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (getModel().isRollover()) {
                g2.setColor(SIDEBAR_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }

            Icon icon = getIcon();
            if (icon != null) {
                icon.paintIcon(this, g2, 13, (getHeight() - icon.getIconHeight()) / 2);
            }
            g2.setFont(font(13, isSelected() ? Font.BOLD : Font.PLAIN));
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), 46, baseline);
            g2.dispose();
        }
    }

    /** 颜色预览 / 取色按钮。 */
    public static class ColorChip extends JButton {
        private Color color;
        private final int arc;

        public ColorChip(Color color, int size, int arc) {
            this.color = color;
            this.arc = arc;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setToolTipText("点击选择自定义颜色");
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int inset = 2;
            g2.setColor(color);
            g2.fillRoundRect(inset, inset, getWidth() - inset * 2,
                    getHeight() - inset * 2, arc, arc);
            g2.setColor(color.getRed() + color.getGreen() + color.getBlue() > 690
                    ? new Color(0xCFC8BA) : alpha(Color.BLACK, 35));
            g2.drawRoundRect(inset, inset, getWidth() - inset * 2 - 1,
                    getHeight() - inset * 2 - 1, arc, arc);
            if (getModel().isRollover()) {
                g2.setColor(alpha(Color.WHITE, 105));
                g2.drawRoundRect(inset + 1, inset + 1, getWidth() - inset * 2 - 3,
                        getHeight() - inset * 2 - 3, arc, arc);
            }
            g2.dispose();
        }
    }

    /** 音乐卡片、提示卡片使用的圆角面板。 */
    public static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color border;
        private final int arc;

        public RoundedPanel(LayoutManager layout, Color fill, Color border, int arc) {
            super(layout);
            this.fill = fill;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
