package com.drawstudio;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 一个可以被序列化、撤销和重绘的图形对象。 */
public class DrawShape implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        PENCIL, LINE, RECT, ELLIPSE, TRIANGLE, POLYGON, TEXT, ERASER
    }

    private final Type type;
    private int x1;
    private int y1;
    private int x2;
    private int y2;
    private Color strokeColor;
    private Color fillColor;
    private float strokeWidth;
    private boolean filled;
    private String text;
    private Font font;
    private final List<Point> points = new ArrayList<Point>();

    public DrawShape(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public int getX1() {
        return x1;
    }

    public void setStart(int x, int y) {
        this.x1 = x;
        this.y1 = y;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public void setEnd(int x, int y) {
        this.x2 = x;
        this.y2 = y;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void addPoint(int x, int y) {
        points.add(new Point(x, y));
    }

    public void paint(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (type == Type.TEXT) {
            paintText(g);
            g.dispose();
            return;
        }

        float width = Math.max(1f, strokeWidth);
        if (type == Type.ERASER) {
            width = Math.max(22f, strokeWidth * 4f);
            g.setColor(Color.WHITE);
        } else {
            g.setColor(strokeColor != null ? strokeColor : Theme.INK);
        }
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (type == Type.PENCIL || type == Type.ERASER) {
            paintFreehand(g, width);
            g.dispose();
            return;
        }

        int left = Math.min(x1, x2);
        int top = Math.min(y1, y2);
        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);

        switch (type) {
            case LINE:
                g.drawLine(x1, y1, x2, y2);
                break;
            case RECT: {
                Shape shape = new RoundRectangle2D.Float(left, top, w, h, 2f, 2f);
                paintFilled(g, shape);
                break;
            }
            case ELLIPSE: {
                Shape shape = new Ellipse2D.Float(left, top, w, h);
                paintFilled(g, shape);
                break;
            }
            case TRIANGLE: {
                Path2D path = new Path2D.Float();
                path.moveTo(left + w / 2.0, top);
                path.lineTo(left + w, top + h);
                path.lineTo(left, top + h);
                path.closePath();
                paintFilled(g, path);
                break;
            }
            case POLYGON: {
                Path2D path = new Path2D.Float();
                double cx = left + w / 2.0;
                double cy = top + h / 2.0;
                double rx = Math.max(1, w / 2.0);
                double ry = Math.max(1, h / 2.0);
                for (int i = 0; i < 5; i++) {
                    double angle = -Math.PI / 2 + i * 2 * Math.PI / 5;
                    double px = cx + Math.cos(angle) * rx;
                    double py = cy + Math.sin(angle) * ry;
                    if (i == 0) {
                        path.moveTo(px, py);
                    } else {
                        path.lineTo(px, py);
                    }
                }
                path.closePath();
                paintFilled(g, path);
                break;
            }
            default:
                break;
        }
        g.dispose();
    }

    private void paintFilled(Graphics2D g, Shape shape) {
        if (filled && fillColor != null) {
            g.setColor(fillColor);
            g.fill(shape);
            g.setColor(strokeColor != null ? strokeColor : Theme.INK);
        }
        g.draw(shape);
    }

    private void paintFreehand(Graphics2D g, float width) {
        if (points.isEmpty()) {
            return;
        }
        if (points.size() == 1) {
            Point p = points.get(0);
            int d = Math.max(2, Math.round(width));
            if (type == Type.ERASER) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(strokeColor != null ? strokeColor : Theme.INK);
            }
            g.fillOval(p.x - d / 2, p.y - d / 2, d, d);
            return;
        }
        Path2D path = new Path2D.Float();
        Point first = points.get(0);
        path.moveTo(first.x, first.y);
        for (int i = 1; i < points.size(); i++) {
            Point p = points.get(i);
            path.lineTo(p.x, p.y);
        }
        g.draw(path);
    }

    private void paintText(Graphics2D g) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Font drawFont = font != null ? font : Theme.font(32);
        g.setFont(drawFont);
        g.setColor(strokeColor != null ? strokeColor : Theme.INK);
        FontMetrics fm = g.getFontMetrics();
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\\n", -1);
        int lineHeight = fm.getHeight();
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], x1, y1 + fm.getAscent() + i * lineHeight);
        }
    }
}
