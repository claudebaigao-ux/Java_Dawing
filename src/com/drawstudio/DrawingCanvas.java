package com.drawstudio;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 绘图画布：处理鼠标交互、图形列表、文字输入、撤销重做以及文件的读写。
 */
public class DrawingCanvas extends JPanel implements MouseListener, MouseMotionListener {

    public enum Tool {
        PENCIL, LINE, RECT, ELLIPSE, TRIANGLE, POLYGON, TEXT, ERASER
    }

    private static final Map<Tool, String> TOOL_NAMES = new LinkedHashMap<Tool, String>();
    private static final Map<Tool, String> TOOL_HINTS = new LinkedHashMap<Tool, String>();

    static {
        TOOL_NAMES.put(Tool.PENCIL, "铅笔");
        TOOL_NAMES.put(Tool.LINE, "直线");
        TOOL_NAMES.put(Tool.RECT, "矩形");
        TOOL_NAMES.put(Tool.ELLIPSE, "椭圆");
        TOOL_NAMES.put(Tool.TRIANGLE, "三角形");
        TOOL_NAMES.put(Tool.POLYGON, "多边形");
        TOOL_NAMES.put(Tool.TEXT, "文字");
        TOOL_NAMES.put(Tool.ERASER, "橡皮擦");

        TOOL_HINTS.put(Tool.PENCIL, "按住鼠标左键拖动，自由绘制线条");
        TOOL_HINTS.put(Tool.LINE, "拖动绘制直线，按住 Shift 可锁定 45° 方向");
        TOOL_HINTS.put(Tool.RECT, "拖动绘制矩形，按住 Shift 可绘制正方形");
        TOOL_HINTS.put(Tool.ELLIPSE, "拖动绘制椭圆，按住 Shift 可绘制正圆");
        TOOL_HINTS.put(Tool.TRIANGLE, "拖动绘制三角形，按住 Shift 可保持等宽高");
        TOOL_HINTS.put(Tool.POLYGON, "拖动绘制五边形，按住 Shift 可保持等宽高");
        TOOL_HINTS.put(Tool.TEXT, "在画布上单击，输入文字并设置字体和颜色");
        TOOL_HINTS.put(Tool.ERASER, "按住鼠标左键拖动，擦除画布内容");
    }

    private final List<DrawShape> shapes = new ArrayList<DrawShape>();
    private final Deque<List<DrawShape>> undoStack = new ArrayDeque<List<DrawShape>>();
    private final Deque<List<DrawShape>> redoStack = new ArrayDeque<List<DrawShape>>();
    private static final int MAX_HISTORY = 80;

    private Tool tool = Tool.PENCIL;
    private Color strokeColor = Theme.INK;
    private Color fillColor = Theme.ACCENT_SOFT;
    private Color textColor = Theme.INK;
    private boolean fillEnabled = true;
    private float strokeWidth = 3f;
    private Font textFont = Theme.font(34, Font.PLAIN);

    private DrawShape preview;
    private boolean drawing;
    private boolean dirty;
    private Point startPoint;
    private Point currentPoint;
    private Point textPoint;

    private Runnable onChange;
    private Consumer<Point> onCoordinate;

    public DrawingCanvas() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setPreferredSize(new Dimension(900, 640));
        setMinimumSize(new Dimension(480, 360));
        addMouseListener(this);
        addMouseMotionListener(this);

        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelDrawing");
        getActionMap().put("cancelDrawing", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelDrawing();
            }
        });
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void setOnCoordinate(Consumer<Point> onCoordinate) {
        this.onCoordinate = onCoordinate;
    }

    public Tool getTool() {
        return tool;
    }

    public String getToolName() {
        return TOOL_NAMES.get(tool);
    }

    public String getToolHint() {
        return TOOL_HINTS.get(tool);
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        this.preview = null;
        this.drawing = false;
        setCursor(Cursor.getPredefinedCursor(tool == Tool.TEXT
                ? Cursor.TEXT_CURSOR : Cursor.CROSSHAIR_CURSOR));
        repaint();
    }

    public void setStrokeColor(Color color) {
        this.strokeColor = color;
    }

    public void setFillColor(Color color) {
        this.fillColor = color;
    }

    public void setTextColor(Color color) {
        this.textColor = color;
    }

    public void setTextStyle(Font font, Color color) {
        this.textFont = font;
        this.textColor = color;
    }

    public Font getTextFont() {
        return textFont;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setFillEnabled(boolean enabled) {
        this.fillEnabled = enabled;
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
    }

    public int getShapeCount() {
        return shapes.size();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markSaved() {
        dirty = false;
    }

    public void notifyChanged() {
        if (onChange != null) {
            onChange.run();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        for (DrawShape shape : shapes) {
            shape.paint(g);
        }
        if (preview != null) {
            preview.paint(g);
        }
        if (shapes.isEmpty() && preview == null) {
            paintEmptyState(g);
        }

        g.setColor(new Color(0xE0DBCF));
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        g.dispose();
    }

    private void paintEmptyState(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        if (w < 340 || h < 220) {
            return;
        }
        int boxW = Math.min(440, w - 90);
        int boxH = 112;
        int x = (w - boxW) / 2;
        int y = (h - boxH) / 2;
        g.setColor(new Color(0xFBFAF6));
        g.fill(new RoundRectangle2D.Float(x, y, boxW, boxH, 18, 18));
        g.setColor(new Color(0xEDE7DB));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, boxW - 1, boxH - 1, 18, 18));

        g.setColor(new Color(0xF8E7BE));
        g.fillOval(x + 22, y + 27, 58, 58);
        Icons.icon(Icons.Kind.PENCIL, 30).paintIcon(this, g, x + 36, y + 41);

        g.setFont(Theme.font(15, Font.BOLD));
        g.setColor(Theme.INK);
        g.drawString("从一个工具开始你的创作", x + 98, y + 43);
        g.setFont(Theme.font(12));
        g.setColor(Theme.MUTED);
        g.drawString("直线 · 矩形 · 椭圆 · 三角形 · 多边形 · 文字", x + 98, y + 66);
        g.setColor(new Color(0xE9AE45));
        g.fillRoundRect(x + 98, y + 79, 34, 3, 3, 3);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        requestFocusInWindow();
        Point p = clamp(e.getPoint());
        if (tool == Tool.TEXT) {
            textPoint = p;
            return;
        }

        startPoint = p;
        currentPoint = p;
        drawing = true;
        preview = null;
        if (tool == Tool.PENCIL || tool == Tool.ERASER) {
            preview = newFreehandShape(p);
            preview.addPoint(p.x, p.y);
        }
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = clamp(e.getPoint());
        if (onCoordinate != null) {
            onCoordinate.accept(p);
        }
        if (!drawing) {
            return;
        }
        Point constrained = constrain(startPoint, p, e.isShiftDown());
        currentPoint = constrained;

        if (tool == Tool.PENCIL || tool == Tool.ERASER) {
            if (preview == null) {
                preview = newFreehandShape(constrained);
            }
            List<Point> pts = preview.getPoints();
            Point last = pts.isEmpty() ? null : pts.get(pts.size() - 1);
            if (last == null || last.distance(constrained) >= 1.2) {
                preview.addPoint(constrained.x, constrained.y);
            }
        } else {
            preview = newGeometryShape(tool, startPoint, constrained);
        }
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        if (tool == Tool.TEXT) {
            Point p = textPoint != null ? textPoint : clamp(e.getPoint());
            textPoint = null;
            if (isInsideCanvas(p)) {
                openTextDialog(p);
            }
            return;
        }
        if (!drawing) {
            return;
        }
        drawing = false;
        Point p = clamp(e.getPoint());
        Point constrained = constrain(startPoint, p, e.isShiftDown());
        currentPoint = constrained;

        if (tool == Tool.PENCIL || tool == Tool.ERASER) {
            if (preview != null) {
                List<Point> pts = preview.getPoints();
                if (!pts.isEmpty()) {
                    Point last = pts.get(pts.size() - 1);
                    if (last.distance(constrained) >= 0.8) {
                        preview.addPoint(constrained.x, constrained.y);
                    }
                }
                commit(preview);
            }
        } else {
            DrawShape shape = newGeometryShape(tool, startPoint, constrained);
            if (isMeaningful(shape)) {
                commit(shape);
            } else {
                preview = null;
                repaint();
            }
        }
        preview = null;
        startPoint = null;
        currentPoint = null;
    }

    private boolean isMeaningful(DrawShape shape) {
        if (shape.getType() == DrawShape.Type.LINE) {
            return Math.hypot(shape.getX2() - shape.getX1(), shape.getY2() - shape.getY1()) >= 2.5;
        }
        return Math.abs(shape.getX2() - shape.getX1()) >= 2
                && Math.abs(shape.getY2() - shape.getY1()) >= 2;
    }

    private void openTextDialog(Point p) {
        TextDialog.Result result = TextDialog.showDialog(SwingUtilities.getWindowAncestor(this),
                textFont, textColor);
        if (result == null) {
            return;
        }
        textFont = result.font;
        textColor = result.color;
        DrawShape shape = new DrawShape(DrawShape.Type.TEXT);
        shape.setStart(p.x, p.y);
        shape.setEnd(p.x, p.y);
        shape.setText(result.text);
        shape.setFont(result.font);
        shape.setStrokeColor(result.color);
        shape.setStrokeWidth(1f);
        commit(shape);
    }

    private DrawShape newFreehandShape(Point p) {
        DrawShape shape = new DrawShape(tool == Tool.ERASER
                ? DrawShape.Type.ERASER : DrawShape.Type.PENCIL);
        shape.setStart(p.x, p.y);
        shape.setEnd(p.x, p.y);
        shape.setStrokeColor(strokeColor);
        shape.setStrokeWidth(strokeWidth);
        return shape;
    }

    private DrawShape newGeometryShape(Tool currentTool, Point a, Point b) {
        DrawShape.Type type;
        switch (currentTool) {
            case LINE:
                type = DrawShape.Type.LINE;
                break;
            case RECT:
                type = DrawShape.Type.RECT;
                break;
            case ELLIPSE:
                type = DrawShape.Type.ELLIPSE;
                break;
            case TRIANGLE:
                type = DrawShape.Type.TRIANGLE;
                break;
            case POLYGON:
                type = DrawShape.Type.POLYGON;
                break;
            default:
                type = DrawShape.Type.PENCIL;
                break;
        }
        DrawShape shape = new DrawShape(type);
        shape.setStart(a.x, a.y);
        shape.setEnd(b.x, b.y);
        shape.setStrokeColor(strokeColor);
        shape.setStrokeWidth(strokeWidth);
        shape.setFillColor(fillColor);
        shape.setFilled(fillEnabled);
        return shape;
    }

    private Point constrain(Point start, Point end, boolean shift) {
        if (!shift) {
            return end;
        }
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        if (tool == Tool.LINE) {
            double angle = Math.atan2(dy, dx);
            double step = Math.PI / 4;
            angle = Math.round(angle / step) * step;
            double length = Math.hypot(dx, dy);
            int nx = start.x + (int) Math.round(Math.cos(angle) * length);
            int ny = start.y + (int) Math.round(Math.sin(angle) * length);
            return clamp(new Point(nx, ny));
        }
        int size = Math.max(Math.abs(dx), Math.abs(dy));
        int sx = dx < 0 ? -1 : 1;
        int sy = dy < 0 ? -1 : 1;
        return clamp(new Point(start.x + sx * size, start.y + sy * size));
    }

    private void commit(DrawShape shape) {
        if (shape == null) {
            return;
        }
        rememberForUndo();
        shapes.add(shape);
        dirty = true;
        preview = null;
        if (onChange != null) {
            onChange.run();
        }
        repaint();
    }

    private void rememberForUndo() {
        undoStack.push(copyShapes(shapes));
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private List<DrawShape> copyShapes(List<DrawShape> source) {
        return new ArrayList<DrawShape>(source);
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        redoStack.push(copyShapes(shapes));
        shapes.clear();
        shapes.addAll(undoStack.pop());
        dirty = true;
        preview = null;
        if (onChange != null) {
            onChange.run();
        }
        repaint();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        undoStack.push(copyShapes(shapes));
        shapes.clear();
        shapes.addAll(redoStack.pop());
        dirty = true;
        preview = null;
        if (onChange != null) {
            onChange.run();
        }
        repaint();
    }

    public void clearCanvas() {
        if (shapes.isEmpty()) {
            return;
        }
        rememberForUndo();
        shapes.clear();
        preview = null;
        dirty = true;
        if (onChange != null) {
            onChange.run();
        }
        repaint();
    }

    public void newCanvas() {
        shapes.clear();
        undoStack.clear();
        redoStack.clear();
        preview = null;
        dirty = false;
        if (onChange != null) {
            onChange.run();
        }
        repaint();
    }

    public void cancelDrawing() {
        drawing = false;
        preview = null;
        startPoint = null;
        currentPoint = null;
        textPoint = null;
        repaint();
    }

    public void saveProject(File file) throws IOException {
        ProjectData data = new ProjectData();
        data.shapes = new ArrayList<DrawShape>(shapes);
        data.pageWidth = getWidth();
        data.pageHeight = getHeight();
        OutputStream out = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(out);
        try {
            oos.writeObject(data);
        } finally {
            oos.close();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadProject(File file) throws IOException, ClassNotFoundException {
        InputStream in = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(in);
        Object object;
        try {
            object = ois.readObject();
        } finally {
            ois.close();
        }
        List<DrawShape> loaded;
        if (object instanceof ProjectData) {
            loaded = ((ProjectData) object).shapes;
        } else if (object instanceof List) {
            loaded = (List<DrawShape>) object;
        } else {
            throw new IOException("不是有效的澔鼠の奇妙喵屋作品文件");
        }
        shapes.clear();
        if (loaded != null) {
            shapes.addAll(loaded);
        }
        undoStack.clear();
        redoStack.clear();
        preview = null;
        dirty = false;
        if (onChange != null) {
            onChange.run();
        }
        repaint();
    }

    public void exportPng(File file) throws IOException {
        int width = getWidth() > 0 ? getWidth() : 1200;
        int height = getHeight() > 0 ? getHeight() : 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for (DrawShape shape : shapes) {
            shape.paint(g);
        }
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    private Point clamp(Point p) {
        int x = Math.max(1, Math.min(getWidth() - 2, p.x));
        int y = Math.max(1, Math.min(getHeight() - 2, p.y));
        return new Point(x, y);
    }

    private boolean isInsideCanvas(Point p) {
        return p.x >= 0 && p.y >= 0 && p.x < getWidth() && p.y < getHeight();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // 单击逻辑统一在 mouseReleased 中处理。
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (onCoordinate != null) {
            onCoordinate.accept(clamp(e.getPoint()));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (onCoordinate != null) {
            onCoordinate.accept(null);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (onCoordinate != null) {
            onCoordinate.accept(e.getPoint());
        }
    }

    /** 作品文件结构，方便以后扩展画布尺寸等信息。 */
    public static class ProjectData implements Serializable {
        private static final long serialVersionUID = 1L;
        public List<DrawShape> shapes = new ArrayList<DrawShape>();
        public int pageWidth;
        public int pageHeight;
    }
}
