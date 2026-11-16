package com.drawstudio;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 澔鼠の奇妙喵屋主窗口：负责菜单、工具栏、属性栏、状态栏以及各类文件操作。 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final DrawingCanvas canvas;
    private final MusicPlayer musicPlayer;

    private File currentFile;
    private File lastDirectory;
    private boolean updatingMusicControls;
    private boolean exiting;

    private Color strokeColor = Theme.INK;
    private Color fillColor = Theme.ACCENT_SOFT;
    private Color textColor = Theme.INK;
    private boolean fillEnabled = true;

    private final Map<DrawingCanvas.Tool, Theme.ToolButton> toolButtons =
            new LinkedHashMap<DrawingCanvas.Tool, Theme.ToolButton>();

    private Theme.RoundedButton undoButton;
    private Theme.RoundedButton redoButton;
    private Theme.RoundedButton clearButton;
    private Theme.RoundedButton playPauseButton;
    private Theme.ColorChip strokeChip;
    private Theme.ColorChip fillChip;
    private Theme.ColorChip textChip;
    private JCheckBox fillCheck;
    private JCheckBox loopCheck;
    private JComboBox<String> fontFamilyCombo;
    private JComboBox<String> fontStyleCombo;
    private JSpinner fontSizeSpinner;
    private JLabel textPreview;
    private JLabel widthValueLabel;

    private JLabel toolStatusLabel;
    private JLabel coordinateLabel;
    private JLabel countLabel;
    private JLabel messageLabel;
    private JLabel musicStatusLabel;
    private JLabel musicTrackLabel;
    private JLabel musicCountLabel;

    private Timer messageTimer;

    public MainFrame() {
        super("澔鼠の奇妙喵屋 — Java Swing 绘图软件");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 650));
        setIconImage(Icons.appIcon(64));

        canvas = new DrawingCanvas();
        musicPlayer = new MusicPlayer();

        buildUi();
        setJMenuBar(buildMenuBar());
        installShortcuts();

        canvas.setOnChange(this::handleCanvasChange);
        canvas.setOnCoordinate(this::updateCoordinate);
        musicPlayer.setListener(player -> updateMusicUi());

        handleCanvasChange();
        updateToolStatus();
        updateCoordinate(null);
        sizeAndCenter();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        SwingUtilities.invokeLater(() -> {
            updateMusicUi();
            boolean autoplayEnabled = !"false".equalsIgnoreCase(
                    System.getProperty("drawstudio.autoplay", "true"));
            if (autoplayEnabled && musicPlayer.getTrackCount() > 0) {
                Timer autoplay = new Timer(850, e -> {
                    if (musicPlayer.getState() == MusicPlayer.State.STOPPED) {
                        musicPlayer.play();
                    }
                });
                autoplay.setRepeats(false);
                autoplay.start();
            }
        });
    }

    private void sizeAndCenter() {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = Math.min(1400, Math.max(1120, bounds.width - 90));
        int height = Math.min(900, Math.max(670, bounds.height - 80));
        setSize(width, height);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.WINDOW_BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel palette = buildToolPalette();
        body.add(palette, BorderLayout.WEST);

        JPanel canvasWrap = new JPanel(new BorderLayout());
        canvasWrap.setOpaque(false);
        canvasWrap.setBorder(new EmptyBorder(0, 16, 0, 16));
        canvasWrap.add(new CanvasStage(canvas), BorderLayout.CENTER);
        body.add(canvasWrap, BorderLayout.CENTER);

        JScrollPane properties = buildPropertyPanel();
        body.add(properties, BorderLayout.EAST);
        root.add(body, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.SURFACE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(13, 22, 13, 22)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        JLabel logo = new JLabel(new ImageIcon(Icons.appIcon(44)));
        brand.add(logo);
        brand.add(Box.createHorizontalStrut(12));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("澔鼠の奇妙喵屋");
        title.setFont(Theme.font(22, Font.BOLD));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel("JAVA SWING 绘图工作室");
        subtitle.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 10));
        subtitle.setForeground(Theme.MUTED);
        titles.add(title);
        titles.add(Box.createVerticalStrut(1));
        titles.add(subtitle);
        brand.add(titles);
        header.add(brand, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        undoButton = headerIconButton(Icons.Kind.UNDO, "撤销 (Ctrl+Z)", this::undo);
        redoButton = headerIconButton(Icons.Kind.REDO, "重做 (Ctrl+Y)", this::redo);
        clearButton = headerIconButton(Icons.Kind.TRASH, "清空画布", this::clearCanvas);
        actions.add(undoButton);
        actions.add(redoButton);
        actions.add(clearButton);
        actions.add(verticalSeparator(22));
        actions.add(headerIconButton(Icons.Kind.NEW, "新建作品 (Ctrl+N)", this::newProject));
        actions.add(headerIconButton(Icons.Kind.OPEN, "打开作品 (Ctrl+O)", this::openProject));

        Theme.RoundedButton saveButton = new Theme.RoundedButton(
                "保存", Icons.icon(Icons.Kind.SAVE, 16),
                Theme.SURFACE_ALT, Theme.INK, Theme.BORDER, 11);
        saveButton.setPreferredSize(new Dimension(86, 36));
        saveButton.setToolTipText("保存作品 (Ctrl+S)");
        saveButton.addActionListener(e -> saveProject());
        actions.add(saveButton);

        Theme.RoundedButton exportButton = new Theme.RoundedButton(
                "导出 PNG", Icons.icon(Icons.Kind.EXPORT, 16), Theme.ACCENT, Theme.INK);
        exportButton.setPreferredSize(new Dimension(112, 36));
        exportButton.setToolTipText("把画布导出为 PNG 图片 (Ctrl+E)");
        exportButton.addActionListener(e -> exportImage());
        actions.add(exportButton);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private Theme.RoundedButton headerIconButton(Icons.Kind kind, String tooltip, Runnable action) {
        Theme.RoundedButton button = new Theme.RoundedButton("", Icons.icon(kind, 17),
                Theme.SURFACE_ALT, Theme.INK, Theme.BORDER, 10);
        button.setPreferredSize(new Dimension(36, 36));
        button.setBorder(new EmptyBorder(7, 7, 7, 7));
        button.setToolTipText(tooltip);
        button.addActionListener(e -> action.run());
        return button;
    }

    private JComponent verticalSeparator(int height) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(9, height));
        panel.setBorder(new MatteBorder(0, 4, 0, 4, Theme.WINDOW_BG));
        return panel;
    }

    private JPanel buildToolPalette() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(18, 12, 16, 12));
        sidebar.setPreferredSize(new Dimension(162, 100));

        JLabel heading = new JLabel("绘图工具");
        heading.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 11));
        heading.setForeground(Theme.SIDEBAR_MUTED);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(heading);
        sidebar.add(Box.createVerticalStrut(12));

        ButtonGroup group = new ButtonGroup();
        Object[][] specs = {
                {DrawingCanvas.Tool.PENCIL, "铅笔", Icons.Kind.PENCIL, "Alt+1"},
                {DrawingCanvas.Tool.LINE, "直线", Icons.Kind.LINE, "Alt+2"},
                {DrawingCanvas.Tool.RECT, "矩形", Icons.Kind.RECT, "Alt+3"},
                {DrawingCanvas.Tool.ELLIPSE, "椭圆", Icons.Kind.ELLIPSE, "Alt+4"},
                {DrawingCanvas.Tool.TRIANGLE, "三角形", Icons.Kind.TRIANGLE, "Alt+5"},
                {DrawingCanvas.Tool.POLYGON, "多边形", Icons.Kind.POLYGON, "Alt+6"},
                {DrawingCanvas.Tool.TEXT, "文字", Icons.Kind.TEXT, "Alt+7"},
                {DrawingCanvas.Tool.ERASER, "橡皮擦", Icons.Kind.ERASER, "Alt+8"}
        };
        for (Object[] spec : specs) {
            DrawingCanvas.Tool tool = (DrawingCanvas.Tool) spec[0];
            String name = (String) spec[1];
            Icons.Kind kind = (Icons.Kind) spec[2];
            String shortcut = (String) spec[3];
            Theme.ToolButton button = new Theme.ToolButton(name, Icons.icon(kind, 18));
            button.setToolTipText(name + "  (" + shortcut + ")");
            group.add(button);
            toolButtons.put(tool, button);
            button.addActionListener(e -> {
                if (button.isSelected()) {
                    selectTool(tool);
                }
            });
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(4));
        }
        toolButtons.get(DrawingCanvas.Tool.PENCIL).setSelected(true);
        sidebar.add(Box.createVerticalGlue());

        JPanel tipPanel = new JPanel();
        tipPanel.setOpaque(false);
        tipPanel.setLayout(new BoxLayout(tipPanel, BoxLayout.Y_AXIS));
        tipPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tipTitle = new JLabel("小技巧");
        tipTitle.setFont(Theme.font(10, Font.BOLD));
        tipTitle.setForeground(Theme.ACCENT);
        JTextArea tip = new JTextArea("按住 Shift 可画正方形、正圆和 45° 直线。");
        tip.setEditable(false);
        tip.setFocusable(false);
        tip.setOpaque(true);
        tip.setBackground(Theme.SIDEBAR);
        tip.setLineWrap(true);
        tip.setWrapStyleWord(true);
        tip.setFont(Theme.font(10));
        tip.setForeground(Theme.SIDEBAR_MUTED);
        tip.setBorder(new EmptyBorder(0, 0, 0, 0));
        tip.setMaximumSize(new Dimension(132, 44));
        tip.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipPanel.add(tipTitle);
        tipPanel.add(Box.createVerticalStrut(3));
        tipPanel.add(tip);
        tipPanel.add(Box.createVerticalStrut(8));
        JLabel credit = new JLabel("Java Swing 课程作品");
        credit.setFont(Theme.font(9));
        credit.setForeground(new Color(0x6F827C));
        credit.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipPanel.add(credit);
        sidebar.add(tipPanel);
        return sidebar;
    }

    private JScrollPane buildPropertyPanel() {
        JPanel content = new JPanel();
        content.setBackground(Theme.SURFACE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 20, 14, 18));

        content.add(sectionHeader("颜色与线条", null));
        content.add(Box.createVerticalStrut(10));

        strokeChip = new Theme.ColorChip(strokeColor, 28, 9);
        strokeChip.addActionListener(e -> chooseStrokeColor());
        content.add(labeledRow("描边颜色", strokeChip));
        content.add(Box.createVerticalStrut(7));
        content.add(buildSwatchRow(color -> setStrokeColor(color)));
        content.add(Box.createVerticalStrut(12));

        fillCheck = new JCheckBox("填充图形");
        fillCheck.setSelected(fillEnabled);
        fillCheck.setOpaque(false);
        fillCheck.setFont(Theme.font(13));
        fillCheck.setForeground(Theme.INK);
        fillCheck.setFocusPainted(false);
        fillChip = new Theme.ColorChip(fillColor, 28, 9);
        fillChip.addActionListener(e -> chooseFillColor());
        JPanel fillRow = new JPanel(new BorderLayout());
        fillRow.setOpaque(false);
        fillRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        fillRow.add(fillCheck, BorderLayout.WEST);
        fillRow.add(fillChip, BorderLayout.EAST);
        fillRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        content.add(fillRow);
        content.add(Box.createVerticalStrut(5));
        content.add(buildSwatchRow(color -> setFillColor(color, true)));
        content.add(Box.createVerticalStrut(12));

        content.add(buildWidthControl());
        content.add(Box.createVerticalStrut(12));
        content.add(separator());
        content.add(Box.createVerticalStrut(12));

        content.add(sectionHeader("文字样式", "用于新文字"));
        content.add(Box.createVerticalStrut(5));
        JLabel textHint = Theme.muted("选择文字工具后，在画布上单击输入", 11);
        textHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(textHint);
        content.add(Box.createVerticalStrut(8));

        fontFamilyCombo = buildFontFamilyCombo();
        content.add(fontFamilyCombo);
        content.add(Box.createVerticalStrut(8));

        fontStyleCombo = new JComboBox<String>(new String[]{"常规", "粗体", "斜体", "粗斜体"});
        fontStyleCombo.setFont(Theme.font(13));
        fontStyleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        fontStyleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(34, 8, 200, 1));
        fontSizeSpinner.setEditor(new JSpinner.NumberEditor(fontSizeSpinner, "0"));
        fontSizeSpinner.setFont(Theme.font(13));
        fontSizeSpinner.setPreferredSize(new Dimension(74, 30));
        JPanel styleRow = new JPanel(new GridLayout(1, 2, 8, 0));
        styleRow.setOpaque(false);
        styleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        styleRow.add(labeledField("字形", fontStyleCombo));
        styleRow.add(labeledField("字号", fontSizeSpinner));
        content.add(styleRow);
        content.add(Box.createVerticalStrut(8));

        textChip = new Theme.ColorChip(textColor, 28, 9);
        textChip.addActionListener(e -> chooseTextColor());
        content.add(labeledRow("文字颜色", textChip));
        content.add(Box.createVerticalStrut(8));

        textPreview = new JLabel("Aa 澔鼠の奇妙喵屋");
        textPreview.setOpaque(true);
        textPreview.setBackground(new Color(0xFBF8F1));
        textPreview.setForeground(textColor);
        textPreview.setFont(Theme.font(22));
        textPreview.setBorder(new CompoundBorder(
                new LineBorder(Theme.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        textPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        content.add(textPreview);
        content.add(Box.createVerticalStrut(12));
        content.add(separator());
        content.add(Box.createVerticalStrut(12));

        content.add(buildMusicSection());

        fontFamilyCombo.addActionListener(e -> applyTextStyle());
        fontStyleCombo.addActionListener(e -> applyTextStyle());
        fontSizeSpinner.addChangeListener(e -> applyTextStyle());
        fillCheck.addActionListener(e -> {
            fillEnabled = fillCheck.isSelected();
            canvas.setFillEnabled(fillEnabled);
        });

        applyTextStyle();

        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(302, 100));
        scroll.setBorder(new MatteBorder(0, 1, 0, 0, Theme.BORDER));
        scroll.setBackground(Theme.SURFACE);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel sectionHeader(String title, String hint) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel label = Theme.sectionTitle(title);
        row.add(label, BorderLayout.WEST);
        if (hint != null) {
            JLabel right = Theme.muted(hint, 10);
            row.add(right, BorderLayout.EAST);
        }
        return row;
    }

    private JPanel labeledRow(String labelText, JComponent right) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.font(13));
        label.setForeground(Theme.INK);
        row.add(label, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JPanel labeledField(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(Theme.font(11));
        label.setForeground(Theme.MUTED);
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildSwatchRow(Consumer<Color> picker) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        Color[] swatches = {
                new Color(0x22302D), new Color(0xFFFFFF), new Color(0x8C8C86),
                new Color(0xBE5D4D), new Color(0xE9AE45), new Color(0xF4D77D),
                new Color(0x67A58D), new Color(0x2F6E64), new Color(0x5B7DB1),
                new Color(0x8A6A9E)
        };
        for (Color color : swatches) {
            Theme.ColorChip chip = new Theme.ColorChip(color, 21, 7);
            chip.setToolTipText("使用这个颜色");
            chip.addActionListener(e -> picker.accept(color));
            row.add(chip);
        }
        return row;
    }

    private JPanel buildWidthControl() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel labels = new JPanel(new BorderLayout());
        labels.setOpaque(false);
        labels.setAlignmentX(Component.LEFT_ALIGNMENT);
        labels.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel label = new JLabel("线条粗细");
        label.setFont(Theme.font(13));
        label.setForeground(Theme.INK);
        widthValueLabel = new JLabel("3 px");
        widthValueLabel.setFont(Theme.font(11, Font.BOLD));
        widthValueLabel.setForeground(Theme.TEAL);
        labels.add(label, BorderLayout.WEST);
        labels.add(widthValueLabel, BorderLayout.EAST);
        wrapper.add(labels);
        wrapper.add(Box.createVerticalStrut(2));

        JSlider slider = new JSlider(1, 20, 3);
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        slider.addChangeListener(e -> {
            int value = slider.getValue();
            widthValueLabel.setText(value + " px");
            canvas.setStrokeWidth(value);
        });
        wrapper.add(slider);
        return wrapper;
    }

    private JComboBox<String> buildFontFamilyCombo() {
        String[] all = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        Arrays.sort(all, String.CASE_INSENSITIVE_ORDER);
        JComboBox<String> combo = new JComboBox<String>(all);
        combo.setFont(Theme.font(13));
        combo.setMaximumRowCount(18);
        combo.setPrototypeDisplayValue("Microsoft YaHei UI");
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean selected = false;
        for (String name : all) {
            if (name.equalsIgnoreCase(Theme.FONT_FAMILY)) {
                combo.setSelectedItem(name);
                selected = true;
                break;
            }
        }
        if (!selected && combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
        return combo;
    }

    private JComponent buildMusicSection() {
        Theme.RoundedPanel card = new Theme.RoundedPanel(new BorderLayout(0, 8),
                new Color(0xF8F4E9), new Color(0xEEE6D5), 14);
        card.setBorder(new EmptyBorder(10, 13, 10, 13));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titleBox.setOpaque(false);
        JLabel icon = new JLabel(Icons.icon(Icons.Kind.MUSIC, 14));
        icon.setForeground(Theme.TEAL);
        JLabel title = new JLabel("背景音乐");
        title.setFont(Theme.font(13, Font.BOLD));
        title.setForeground(Theme.INK);
        titleBox.add(icon);
        titleBox.add(title);
        top.add(titleBox, BorderLayout.WEST);
        musicCountLabel = Theme.muted("0 / 0", 10);
        top.add(musicCountLabel, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        musicTrackLabel = new JLabel("正在载入…");
        musicTrackLabel.setFont(Theme.font(15, Font.BOLD));
        musicTrackLabel.setForeground(Theme.INK);
        musicTrackLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        musicStatusLabel = Theme.muted("准备播放", 10);
        musicStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        middle.add(musicTrackLabel);
        middle.add(Box.createVerticalStrut(2));
        middle.add(musicStatusLabel);
        card.add(middle, BorderLayout.CENTER);

        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        buttons.setOpaque(false);
        Theme.RoundedButton previous = new Theme.RoundedButton("", Icons.icon(Icons.Kind.PREV, 14),
                new Color(0xEFE9DB), Theme.INK, null, 17);
        previous.setPreferredSize(new Dimension(30, 30));
        previous.setBorder(new EmptyBorder(7, 7, 7, 7));
        previous.setToolTipText("上一首");
        previous.addActionListener(e -> musicPlayer.previous());
        playPauseButton = new Theme.RoundedButton("", Icons.icon(Icons.Kind.PLAY, 15),
                Theme.ACCENT, Theme.INK, null, 19);
        playPauseButton.setPreferredSize(new Dimension(36, 36));
        playPauseButton.setBorder(new EmptyBorder(9, 9, 9, 9));
        playPauseButton.setToolTipText("播放 / 暂停");
        playPauseButton.addActionListener(e -> musicPlayer.togglePause());
        Theme.RoundedButton next = new Theme.RoundedButton("", Icons.icon(Icons.Kind.NEXT, 14),
                new Color(0xEFE9DB), Theme.INK, null, 17);
        next.setPreferredSize(new Dimension(30, 30));
        next.setBorder(new EmptyBorder(7, 7, 7, 7));
        next.setToolTipText("下一首");
        next.addActionListener(e -> musicPlayer.next());
        buttons.add(previous);
        buttons.add(playPauseButton);
        buttons.add(next);
        controls.add(buttons, BorderLayout.WEST);

        loopCheck = new JCheckBox("循环");
        loopCheck.setSelected(true);
        loopCheck.setOpaque(false);
        loopCheck.setFont(Theme.font(11));
        loopCheck.setForeground(Theme.INK_SOFT);
        loopCheck.setFocusPainted(false);
        loopCheck.addActionListener(e -> {
            if (!updatingMusicControls) {
                musicPlayer.setLoop(loopCheck.isSelected());
                updateMusicUi();
            }
        });
        controls.add(loopCheck, BorderLayout.EAST);
        card.add(controls, BorderLayout.SOUTH);
        return card;
    }

    private JComponent separator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(Theme.BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        return separator;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(6, 20, 6, 20)));

        toolStatusLabel = new JLabel("工具：铅笔");
        toolStatusLabel.setFont(Theme.font(11));
        toolStatusLabel.setForeground(Theme.INK_SOFT);

        messageLabel = new JLabel("欢迎使用澔鼠の奇妙喵屋，从左侧选择一个工具开始创作");
        messageLabel.setFont(Theme.font(11));
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        coordinateLabel = new JLabel("坐标：—");
        coordinateLabel.setFont(Theme.font(11));
        coordinateLabel.setForeground(Theme.MUTED);
        countLabel = new JLabel("图形：0");
        countLabel.setFont(Theme.font(11));
        countLabel.setForeground(Theme.MUTED);
        right.add(coordinateLabel);
        right.add(countLabel);

        bar.add(toolStatusLabel, BorderLayout.WEST);
        bar.add(messageLabel, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));
        bar.setBackground(Theme.SURFACE);

        JMenu file = new JMenu("文件");
        file.setFont(Theme.font(13, Font.BOLD));
        file.add(menuItem("新建作品", KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, this::newProject));
        file.add(menuItem("打开作品…", KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK, this::openProject));
        file.add(menuItem("保存作品", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, this::saveProject));
        file.add(menuItem("作品另存为…", KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, this::saveProjectAs));
        file.addSeparator();
        file.add(menuItem("导出为 PNG…", KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, this::exportImage));
        file.addSeparator();
        JMenuItem exit = new JMenuItem("退出");
        exit.addActionListener(e -> exitApplication());
        file.add(exit);
        bar.add(file);

        JMenu edit = new JMenu("编辑");
        edit.setFont(Theme.font(13, Font.BOLD));
        edit.add(menuItem("撤销", KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, this::undo));
        edit.add(menuItem("重做", KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK, this::redo));
        edit.add(menuItem("重做（备用快捷键）", KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, this::redo));
        edit.addSeparator();
        JMenuItem clear = new JMenuItem("清空画布");
        clear.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        clear.addActionListener(e -> clearCanvas());
        edit.add(clear);
        bar.add(edit);

        JMenu help = new JMenu("帮助");
        help.setFont(Theme.font(13, Font.BOLD));
        JMenuItem shortcuts = new JMenuItem("快捷键说明");
        shortcuts.addActionListener(e -> showShortcuts());
        JMenuItem about = new JMenuItem("关于澔鼠の奇妙喵屋");
        about.addActionListener(e -> showAbout());
        help.add(shortcuts);
        help.add(about);
        bar.add(help);
        return bar;
    }

    private JMenuItem menuItem(String text, int key, int modifiers, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(Theme.font(13));
        item.setAccelerator(KeyStroke.getKeyStroke(key, modifiers));
        item.addActionListener(e -> action.run());
        return item;
    }

    private void installShortcuts() {
        DrawingCanvas.Tool[] tools = {
                DrawingCanvas.Tool.PENCIL, DrawingCanvas.Tool.LINE, DrawingCanvas.Tool.RECT,
                DrawingCanvas.Tool.ELLIPSE, DrawingCanvas.Tool.TRIANGLE,
                DrawingCanvas.Tool.POLYGON, DrawingCanvas.Tool.TEXT, DrawingCanvas.Tool.ERASER
        };
        int[] keys = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8};
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        for (int i = 0; i < tools.length; i++) {
            final DrawingCanvas.Tool tool = tools[i];
            String key = "selectTool" + i;
            inputMap.put(KeyStroke.getKeyStroke(keys[i], InputEvent.ALT_DOWN_MASK), key);
            actionMap.put(key, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    selectTool(tool);
                }
            });
        }
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelCurrentShape");
        actionMap.put("cancelCurrentShape", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.cancelDrawing();
            }
        });
    }

    private void selectTool(DrawingCanvas.Tool tool) {
        canvas.setTool(tool);
        Theme.ToolButton button = toolButtons.get(tool);
        if (button != null && !button.isSelected()) {
            button.setSelected(true);
        }
        canvas.requestFocusInWindow();
        updateToolStatus();
        showMessage(canvas.getToolHint());
    }

    private void updateToolStatus() {
        if (toolStatusLabel != null) {
            toolStatusLabel.setText("工具：" + canvas.getToolName());
        }
    }

    private void handleCanvasChange() {
        if (undoButton != null) {
            undoButton.setEnabled(canvas.canUndo());
            redoButton.setEnabled(canvas.canRedo());
            clearButton.setEnabled(canvas.getShapeCount() > 0);
        }
        if (countLabel != null) {
            countLabel.setText("图形：" + canvas.getShapeCount());
        }
        updateTitle();
    }

    private void updateTitle() {
        String name = currentFile != null ? currentFile.getName() : "未命名作品";
        setTitle("澔鼠の奇妙喵屋 — " + name + (canvas.isDirty() ? " *" : ""));
    }

    private void updateCoordinate(Point point) {
        if (coordinateLabel == null) {
            return;
        }
        if (point == null) {
            coordinateLabel.setText("坐标：—");
        } else {
            coordinateLabel.setText("坐标：" + point.x + ", " + point.y);
        }
    }

    private void updateMusicUi() {
        if (musicStatusLabel == null) {
            return;
        }
        int count = musicPlayer.getTrackCount();
        MusicPlayer.State state = musicPlayer.getState();
        if (count == 0) {
            musicTrackLabel.setText("未找到音乐");
            musicStatusLabel.setText(musicPlayer.getErrorMessage() != null
                    ? musicPlayer.getErrorMessage() : "请把 MP3 放入 music 文件夹");
            musicCountLabel.setText("0 / 0");
            musicStatusLabel.setForeground(Theme.DANGER);
            playPauseButton.setEnabled(false);
            playPauseButton.setIcon(Icons.icon(Icons.Kind.PLAY, 15));
            return;
        }
        musicTrackLabel.setText(musicPlayer.getTrackName());
        musicStatusLabel.setText(musicPlayer.getStatusText());
        musicCountLabel.setText((musicPlayer.getTrackIndex() + 1) + " / " + count);
        if (state == MusicPlayer.State.ERROR) {
            musicStatusLabel.setForeground(Theme.DANGER);
        } else if (state == MusicPlayer.State.PLAYING) {
            musicStatusLabel.setForeground(Theme.TEAL);
        } else {
            musicStatusLabel.setForeground(Theme.MUTED);
        }
        boolean playing = state == MusicPlayer.State.PLAYING;
        playPauseButton.setIcon(Icons.icon(playing ? Icons.Kind.PAUSE : Icons.Kind.PLAY, 15));
        playPauseButton.setToolTipText(playing ? "暂停背景音乐" : "播放背景音乐");
        playPauseButton.setEnabled(true);
        updatingMusicControls = true;
        loopCheck.setSelected(musicPlayer.isLoop());
        updatingMusicControls = false;
    }

    private void applyTextStyle() {
        if (fontFamilyCombo == null || fontStyleCombo == null || fontSizeSpinner == null) {
            return;
        }
        String family = (String) fontFamilyCombo.getSelectedItem();
        if (family == null) {
            family = Theme.FONT_FAMILY;
        }
        int[] styles = {Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC};
        int style = styles[Math.max(0, fontStyleCombo.getSelectedIndex())];
        int size = ((Number) fontSizeSpinner.getValue()).intValue();
        Font font = new Font(family, style, size);
        canvas.setTextStyle(font, textColor);
        if (textPreview != null) {
            textPreview.setFont(font.deriveFont((float) Math.min(22, size)));
            textPreview.setForeground(textColor);
            textPreview.setText("Aa  " + Theme.APP_NAME);
        }
    }

    private void setStrokeColor(Color color) {
        strokeColor = color;
        if (strokeChip != null) {
            strokeChip.setColor(color);
        }
        canvas.setStrokeColor(color);
    }

    private void setFillColor(Color color, boolean enableFill) {
        fillColor = color;
        if (fillChip != null) {
            fillChip.setColor(color);
        }
        canvas.setFillColor(color);
        if (enableFill && fillCheck != null && !fillCheck.isSelected()) {
            fillCheck.setSelected(true);
            fillEnabled = true;
            canvas.setFillEnabled(true);
        }
    }

    private void setTextColor(Color color) {
        textColor = color;
        if (textChip != null) {
            textChip.setColor(color);
        }
        canvas.setTextColor(color);
        applyTextStyle();
    }

    private void chooseStrokeColor() {
        Color chosen = JColorChooser.showDialog(this, "选择描边颜色", strokeColor);
        if (chosen != null) {
            setStrokeColor(chosen);
        }
    }

    private void chooseFillColor() {
        Color chosen = JColorChooser.showDialog(this, "选择填充颜色", fillColor);
        if (chosen != null) {
            setFillColor(chosen, false);
        }
    }

    private void chooseTextColor() {
        Color chosen = JColorChooser.showDialog(this, "选择文字颜色", textColor);
        if (chosen != null) {
            setTextColor(chosen);
        }
    }

    private void undo() {
        canvas.undo();
        showMessage("已撤销上一步操作");
    }

    private void redo() {
        canvas.redo();
        showMessage("已重做");
    }

    private void clearCanvas() {
        if (canvas.getShapeCount() == 0) {
            showMessage("画布已经是空白的");
            return;
        }
        Object[] options = {"清空画布", "取消"};
        int result = JOptionPane.showOptionDialog(this,
                "确定要清空当前画布吗？清空后仍可按 Ctrl+Z 撤销。",
                "清空画布", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (result == 0) {
            canvas.clearCanvas();
            showMessage("画布已清空，可按 Ctrl+Z 撤销");
        }
    }

    private void newProject() {
        if (!confirmBeforeDiscard()) {
            return;
        }
        canvas.newCanvas();
        currentFile = null;
        updateTitle();
        selectTool(DrawingCanvas.Tool.PENCIL);
        showMessage("已新建空白画布，可以开始创作了");
    }

    private void openProject() {
        if (!confirmBeforeDiscard()) {
            return;
        }
        JFileChooser chooser = createFileChooser();
        chooser.setDialogTitle("打开澔鼠の奇妙喵屋作品");
        chooser.setFileFilter(new FileNameExtensionFilter("澔鼠の奇妙喵屋作品 (*.paint)", "paint"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            canvas.loadProject(file);
            currentFile = file;
            lastDirectory = file.getParentFile();
            canvas.markSaved();
            updateTitle();
            handleCanvasChange();
            showMessage("已打开作品：" + file.getName());
        } catch (Exception ex) {
            showError("无法打开作品", "文件无法读取或格式不正确。\n" + ex.getMessage());
        }
    }

    private boolean saveProject() {
        if (currentFile == null) {
            return saveProjectAs();
        }
        return saveTo(currentFile);
    }

    private boolean saveProjectAs() {
        JFileChooser chooser = createFileChooser();
        chooser.setDialogTitle("保存澔鼠の奇妙喵屋作品");
        chooser.setFileFilter(new FileNameExtensionFilter("澔鼠の奇妙喵屋作品 (*.paint)", "paint"));
        if (currentFile != null) {
            chooser.setSelectedFile(currentFile);
        } else {
            chooser.setSelectedFile(new File("未命名作品.paint"));
        }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File file = ensureExtension(chooser.getSelectedFile(), "paint");
        return saveTo(file);
    }

    private boolean saveTo(File file) {
        try {
            canvas.saveProject(file);
            currentFile = file;
            lastDirectory = file.getParentFile();
            canvas.markSaved();
            updateTitle();
            showMessage("作品已保存：" + file.getName());
            return true;
        } catch (IOException ex) {
            showError("保存失败", "无法写入文件：\n" + ex.getMessage());
            return false;
        }
    }

    private void exportImage() {
        JFileChooser chooser = createFileChooser();
        chooser.setDialogTitle("导出为 PNG 图片");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG 图片 (*.png)", "png"));
        String baseName = currentFile != null ? stripExtension(currentFile.getName()) : "澔鼠の奇妙喵屋作品";
        chooser.setSelectedFile(new File(baseName + ".png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = ensureExtension(chooser.getSelectedFile(), "png");
        try {
            canvas.exportPng(file);
            lastDirectory = file.getParentFile();
            showMessage("图片已导出：" + file.getName());
        } catch (IOException ex) {
            showError("导出失败", "无法写入图片：\n" + ex.getMessage());
        }
    }

    private JFileChooser createFileChooser() {
        JFileChooser chooser = new JFileChooser();
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setCurrentDirectory(lastDirectory);
        } else {
            File desktop = new File(System.getProperty("user.home"), "Desktop");
            if (desktop.isDirectory()) {
                chooser.setCurrentDirectory(desktop);
            }
        }
        chooser.setAcceptAllFileFilterUsed(false);
        return chooser;
    }

    private File ensureExtension(File file, String extension) {
        if (file.getName().toLowerCase().endsWith("." + extension)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + "." + extension);
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private boolean confirmBeforeDiscard() {
        if (!canvas.isDirty()) {
            return true;
        }
        Object[] options = {"保存", "不保存", "取消"};
        int result = JOptionPane.showOptionDialog(this,
                "当前作品还没有保存，要先保存吗？",
                "澔鼠の奇妙喵屋", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (result == 0) {
            return saveProject();
        }
        return result == 1;
    }

    private void showMessage(String message) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(message);
        if (messageTimer != null) {
            messageTimer.stop();
        }
        messageTimer = new Timer(4500, e -> messageLabel.setText(
                "提示：Ctrl+Z 撤销 · Ctrl+S 保存 · 按住 Shift 绘制规整图形"));
        messageTimer.setRepeats(false);
        messageTimer.start();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showShortcuts() {
        String text = "常用快捷键\n\n"
                + "Alt + 1 ~ 8    切换绘图工具\n"
                + "Ctrl + Z       撤销\n"
                + "Ctrl + Y       重做\n"
                + "Ctrl + S       保存作品\n"
                + "Ctrl + O       打开作品\n"
                + "Ctrl + N       新建作品\n"
                + "Ctrl + E       导出 PNG\n"
                + "Shift + 拖动    绘制正方形 / 正圆 / 45° 直线\n"
                + "Esc            取消正在绘制的图形";
        JOptionPane.showMessageDialog(this, text, "快捷键说明", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        String text = "<html><div style='width:340px; font-family:sans-serif;'>"
                + "<h2 style='margin-bottom:4px;'>澔鼠の奇妙喵屋 1.0</h2>"
                + "<p style='color:#68736f;'>一个使用 Java Swing / JFrame 编写的绘图软件。</p>"
                + "<p>支持铅笔、直线、矩形、椭圆、三角形、多边形、文字和橡皮擦；"
                + "可设置描边与填充颜色、线条粗细、文字字体与颜色。</p>"
                + "<p>作品可保存为 .paint 文件，也可以导出 PNG 图片。"
                + "背景音乐来自程序自带的 music 文件夹。</p>"
                + "<p style='color:#8a938f; font-size:11px;'>Java 课程期末作品</p>"
                + "</div></html>";
        JOptionPane.showMessageDialog(this, text, "关于澔鼠の奇妙喵屋", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exitApplication() {
        if (exiting) {
            return;
        }
        if (!confirmBeforeDiscard()) {
            return;
        }
        exiting = true;
        musicPlayer.stop();
        dispose();
        System.exit(0);
    }

    /** 画布外层的舞台：绘制点状背景和纸张阴影。 */
    private static final class CanvasStage extends JPanel {
        private static final long serialVersionUID = 1L;
        private final DrawingCanvas canvas;

        CanvasStage(DrawingCanvas canvas) {
            this.canvas = canvas;
            setBackground(Theme.CANVAS_BG);
            setLayout(null);
            add(canvas);
        }

        @Override
        public void doLayout() {
            int padding = 18;
            canvas.setBounds(padding, padding,
                    Math.max(1, getWidth() - padding * 2),
                    Math.max(1, getHeight() - padding * 2));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xD9D3C7));
            for (int x = 14; x < getWidth(); x += 24) {
                for (int y = 14; y < getHeight(); y += 24) {
                    g.fillOval(x, y, 1, 1);
                }
            }
            int pad = 18;
            int w = Math.max(1, getWidth() - pad * 2);
            int h = Math.max(1, getHeight() - pad * 2);
            g.setColor(new Color(0, 0, 0, 18));
            g.fillRoundRect(pad + 3, pad + 5, w, h, 4, 4);
            g.setColor(new Color(0, 0, 0, 10));
            g.fillRoundRect(pad + 7, pad + 9, w, h, 5, 5);
            g.dispose();
        }
    }
}
