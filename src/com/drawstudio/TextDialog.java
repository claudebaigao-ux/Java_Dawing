package com.drawstudio;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;

/** 输入文字、选择字体与颜色的模态对话框。 */
public final class TextDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextArea textArea = new JTextArea(5, 30);
    private final JComboBox<String> familyCombo;
    private final JComboBox<String> styleCombo;
    private final JSpinner sizeSpinner;
    private final PreviewPanel preview = new PreviewPanel();
    private final JLabel errorLabel = new JLabel(" ");
    private final Theme.ColorChip colorChip;
    private Color textColor;
    private Result result;

    private static final String[] STYLE_NAMES = {"常规", "粗体", "斜体", "粗斜体"};
    private static final int[] STYLE_VALUES = {
            Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC
    };

    public static Result showDialog(Window owner, Font initialFont, Color initialColor) {
        TextDialog dialog = new TextDialog(owner, initialFont, initialColor);
        dialog.setVisible(true);
        return dialog.result;
    }

    private TextDialog(Window owner, Font initialFont, Color initialColor) {
        super(owner, "添加文字", ModalityType.APPLICATION_MODAL);
        this.textColor = initialColor != null ? initialColor : Theme.INK;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(520, 470));
        setSize(570, 540);
        setLocationRelativeTo(owner);

        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        Arrays.sort(families, String.CASE_INSENSITIVE_ORDER);
        familyCombo = new JComboBox<String>(families);
        familyCombo.setMaximumRowCount(18);
        selectFamily(initialFont != null ? initialFont.getFamily() : Theme.FONT_FAMILY);

        styleCombo = new JComboBox<String>(STYLE_NAMES);
        styleCombo.setSelectedIndex(styleIndex(initialFont != null ? initialFont.getStyle() : Font.PLAIN));

        int initialSize = initialFont != null ? initialFont.getSize() : 34;
        initialSize = Math.max(8, Math.min(180, initialSize));
        sizeSpinner = new JSpinner(new SpinnerNumberModel(initialSize, 8, 200, 1));
        sizeSpinner.setEditor(new JSpinner.NumberEditor(sizeSpinner, "0"));
        colorChip = new Theme.ColorChip(this.textColor, 32, 9);
        colorChip.addActionListener(e -> chooseColor());

        buildUi();
        updatePreview();
        SwingUtilities.invokeLater(textArea::requestFocusInWindow);
    }

    private void selectFamily(String family) {
        ComboBoxModel<String> model = familyCombo.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (family.equalsIgnoreCase(model.getElementAt(i))) {
                familyCombo.setSelectedIndex(i);
                return;
            }
        }
        for (int i = 0; i < model.getSize(); i++) {
            if (Theme.FONT_FAMILY.equalsIgnoreCase(model.getElementAt(i))) {
                familyCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private int styleIndex(int style) {
        for (int i = 0; i < STYLE_VALUES.length; i++) {
            if (STYLE_VALUES[i] == style) {
                return i;
            }
        }
        return 0;
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.SURFACE);
        root.setBorder(new EmptyBorder(22, 24, 20, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("添加文字");
        title.setFont(Theme.font(21, Font.BOLD));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel("输入内容，并为这段文字设置字体与颜色");
        subtitle.setFont(Theme.font(12));
        subtitle.setForeground(Theme.MUTED);
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel inputLabel = new JLabel("文字内容");
        inputLabel.setFont(Theme.font(13, Font.BOLD));
        inputLabel.setForeground(Theme.INK);
        inputLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(inputLabel);
        center.add(Box.createVerticalStrut(7));

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(Theme.font(16));
        textArea.setForeground(Theme.INK);
        textArea.setBackground(new Color(0xFCFAF6));
        textArea.setCaretColor(Theme.TEAL);
        textArea.setBorder(new EmptyBorder(9, 11, 9, 11));
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }
        });
        JScrollPane textScroll = new JScrollPane(textArea);
        textScroll.setBorder(new LineBorder(Theme.BORDER, 1, true));
        textScroll.setPreferredSize(new Dimension(480, 104));
        textScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        textScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(textScroll);
        center.add(Box.createVerticalStrut(14));

        JPanel fontRow = new JPanel(new BorderLayout(10, 0));
        fontRow.setOpaque(false);
        fontRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JLabel fontLabel = new JLabel("字体");
        fontLabel.setFont(Theme.font(13, Font.BOLD));
        fontLabel.setForeground(Theme.INK);
        fontLabel.setPreferredSize(new Dimension(44, 28));
        styleCombo.setPreferredSize(new Dimension(90, 30));
        sizeSpinner.setPreferredSize(new Dimension(74, 30));
        JPanel styleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        styleBox.setOpaque(false);
        styleBox.add(styleCombo);
        styleBox.add(sizeSpinner);
        fontRow.add(fontLabel, BorderLayout.WEST);
        fontRow.add(familyCombo, BorderLayout.CENTER);
        fontRow.add(styleBox, BorderLayout.EAST);
        center.add(fontRow);
        center.add(Box.createVerticalStrut(10));

        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        colorRow.setOpaque(false);
        colorRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel colorLabel = new JLabel("文字颜色");
        colorLabel.setFont(Theme.font(13, Font.BOLD));
        colorLabel.setForeground(Theme.INK);
        colorLabel.setPreferredSize(new Dimension(70, 30));
        colorRow.add(colorLabel);
        colorRow.add(colorChip);
        JLabel colorHint = new JLabel("  点击色块自定义颜色");
        colorHint.setFont(Theme.font(11));
        colorHint.setForeground(Theme.MUTED);
        colorRow.add(colorHint);
        center.add(colorRow);
        center.add(Box.createVerticalStrut(14));

        JLabel previewLabel = new JLabel("预览");
        previewLabel.setFont(Theme.font(13, Font.BOLD));
        previewLabel.setForeground(Theme.INK);
        previewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(previewLabel);
        center.add(Box.createVerticalStrut(7));
        preview.setAlignmentX(Component.LEFT_ALIGNMENT);
        preview.setPreferredSize(new Dimension(480, 96));
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 106));
        center.add(preview);
        center.add(Box.createVerticalStrut(6));

        errorLabel.setFont(Theme.font(11));
        errorLabel.setForeground(Theme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(errorLabel);
        root.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel shortcut = new JLabel("Ctrl + Enter 快速添加");
        shortcut.setFont(Theme.font(11));
        shortcut.setForeground(Theme.MUTED);
        footer.add(shortcut, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        Theme.RoundedButton cancel = new Theme.RoundedButton("取消", null,
                Theme.SURFACE_ALT, Theme.INK, Theme.BORDER, 10);
        cancel.addActionListener(e -> dispose());
        Theme.RoundedButton ok = new Theme.RoundedButton("添加到画布", Icons.icon(Icons.Kind.CHECK, 15),
                Theme.ACCENT, Theme.INK);
        ok.addActionListener(e -> confirm());
        buttons.add(cancel);
        buttons.add(ok);
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        familyCombo.addActionListener(e -> {
            updateTextAreaFont();
            updatePreview();
        });
        styleCombo.addActionListener(e -> {
            updateTextAreaFont();
            updatePreview();
        });
        sizeSpinner.addChangeListener(e -> {
            updateTextAreaFont();
            updatePreview();
        });

        setContentPane(root);
        getRootPane().setDefaultButton(ok);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        getRootPane().getActionMap().put("closeDialog", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                        java.awt.event.InputEvent.CTRL_DOWN_MASK), "confirmDialog");
        getRootPane().getActionMap().put("confirmDialog", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                confirm();
            }
        });
    }

    private void chooseColor() {
        Color chosen = JColorChooser.showDialog(this, "选择文字颜色", textColor);
        if (chosen != null) {
            textColor = chosen;
            colorChip.setColor(chosen);
            updatePreview();
        }
    }

    private Font selectedFont() {
        String family = (String) familyCombo.getSelectedItem();
        if (family == null) {
            family = Theme.FONT_FAMILY;
        }
        int style = STYLE_VALUES[Math.max(0, styleCombo.getSelectedIndex())];
        int size = ((Number) sizeSpinner.getValue()).intValue();
        return new Font(family, style, size);
    }

    private void updateTextAreaFont() {
        Font f = selectedFont();
        textArea.setFont(new Font(f.getFamily(), f.getStyle(), Math.min(20, f.getSize())));
    }

    private void updatePreview() {
        preview.repaint();
    }

    private void confirm() {
        String text = textArea.getText().replace("\r\n", "\n").replace('\r', '\n');
        while (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.trim().isEmpty()) {
            errorLabel.setText("请输入文字内容后再添加");
            textArea.requestFocusInWindow();
            return;
        }
        result = new Result(text, selectedFont(), textColor);
        dispose();
    }

    public static final class Result {
        public final String text;
        public final Font font;
        public final Color color;

        Result(String text, Font font, Color color) {
            this.text = text;
            this.font = font;
            this.color = color;
        }
    }

    private final class PreviewPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        PreviewPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xFCFAF5));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.setColor(Theme.BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            String text = textArea.getText();
            if (text.trim().isEmpty()) {
                text = "在画布上留下你的文字…";
                g.setColor(Theme.MUTED);
                g.setFont(Theme.font(14));
                g.drawString(text, 16, getHeight() / 2 + 5);
            } else {
                Font font = selectedFont();
                String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\\n", -1);
                int visibleLines = Math.max(1, Math.min(3, lines.length));
                float maxByHeight = (getHeight() - 18f) / visibleLines - 3f;
                float previewSize = Math.max(9f, Math.min(36f,
                        Math.min(font.getSize2D(), maxByHeight)));
                g.setFont(font.deriveFont(previewSize));
                g.setColor(textColor);
                FontMetrics fm = g.getFontMetrics();
                int lineHeight = fm.getHeight();
                int totalHeight = Math.max(lineHeight, lines.length * lineHeight);
                int y = Math.max(fm.getAscent() + 8, (getHeight() - totalHeight) / 2 + fm.getAscent());
                int maxWidth = getWidth() - 32;
                for (int i = 0; i < lines.length && y < getHeight(); i++) {
                    String line = lines[i];
                    if (fm.stringWidth(line) > maxWidth) {
                        while (line.length() > 1 && fm.stringWidth(line + "…") > maxWidth) {
                            line = line.substring(0, line.length() - 1);
                        }
                        line = line + "…";
                    }
                    g.drawString(line, 16, y);
                    y += lineHeight;
                }
            }
            g.dispose();
        }
    }
}
