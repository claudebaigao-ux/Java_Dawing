package com.drawstudio;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** 功能图标使用 Java2D 绘制；品牌 Logo 使用自带的小猫图片。 */
public final class Icons {

    private static BufferedImage mascotImage;

    public enum Kind {
        PENCIL, LINE, RECT, ELLIPSE, TRIANGLE, POLYGON, TEXT, ERASER,
        UNDO, REDO, SAVE, OPEN, NEW, EXPORT, PLAY, PAUSE, PREV, NEXT,
        LOOP, MUSIC, TRASH, CHECK
    }

    private Icons() {
    }

    public static Icon icon(Kind kind, int size) {
        return new VectorIcon(kind, size);
    }

    /** 品牌 Logo / 窗口图标：把小猫照片裁成圆角方形，并加一圈暖金色描边。 */
    public static Image appIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        BufferedImage mascot = mascotImage();
        if (mascot == null) {
            g.dispose();
            return fallbackAppIcon(size);
        }

        float s = size / 64f;
        g.scale(s, s);
        Shape clip = new RoundRectangle2D.Float(0, 0, 64, 64, 18, 18);
        g.setClip(clip);
        // 等比铺满，保持小猫照片不变形
        double scale = Math.max(64.0 / mascot.getWidth(), 64.0 / mascot.getHeight());
        int drawW = (int) Math.ceil(mascot.getWidth() * scale);
        int drawH = (int) Math.ceil(mascot.getHeight() * scale);
        g.drawImage(mascot, (64 - drawW) / 2, (64 - drawH) / 2, drawW, drawH, null);
        g.setClip(null);

        g.setColor(new Color(0xE9AE45));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(new RoundRectangle2D.Float(0.9f, 0.9f, 62.2f, 62.2f, 16.5f, 16.5f));
        g.dispose();
        return image;
    }

    private static synchronized BufferedImage mascotImage() {
        if (mascotImage == null) {
            InputStream in = Icons.class.getResourceAsStream("/com/drawstudio/mascot.png");
            if (in != null) {
                try {
                    mascotImage = ImageIO.read(in);
                } catch (IOException ignored) {
                    // 图片读取失败时使用下面的矢量回退 Logo。
                } finally {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                        // 忽略关闭异常。
                    }
                }
            }
        }
        return mascotImage;
    }

    /** 资源缺失时的矢量回退图标。 */
    private static Image fallbackAppIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float s = size / 64f;
        g.scale(s, s);

        g.setPaint(new GradientPaint(0, 0, new Color(0x2A3D38), 64, 64, new Color(0x17221F)));
        g.fillRoundRect(0, 0, 64, 64, 18, 18);

        g.setColor(new Color(0xE9AE45));
        g.fillOval(10, 11, 18, 18);
        g.setColor(new Color(0x67A58D));
        g.fillOval(35, 9, 13, 13);
        g.setColor(new Color(0xC8614F));
        g.fillOval(44, 28, 10, 10);

        g.setColor(new Color(0xF9F6EE));
        g.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D brush = new Path2D.Float();
        brush.moveTo(15, 50);
        brush.curveTo(24, 37, 34, 45, 46, 28);
        g.draw(brush);
        g.setColor(new Color(0xE9AE45));
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D accent = new Path2D.Float();
        accent.moveTo(15, 50);
        accent.curveTo(24, 37, 34, 45, 46, 28);
        g.draw(accent);

        g.dispose();
        return image;
    }

    private static final class VectorIcon implements Icon {
        private final Kind kind;
        private final int size;

        VectorIcon(Kind kind, int size) {
            this.kind = kind;
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y);
            double scale = size / 24.0;
            g.scale(scale, scale);

            Color color = c != null ? c.getForeground() : Theme.INK;
            if (c != null && !c.isEnabled()) {
                color = Theme.mix(color, Color.WHITE, 0.55);
            }
            g.setColor(color);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (kind) {
                case PENCIL:
                    pencil(g, color);
                    break;
                case LINE:
                    g.draw(new Line2D.Float(4.5f, 19f, 19.5f, 4.5f));
                    g.fill(new Ellipse2D.Float(3.2f, 17.7f, 2.6f, 2.6f));
                    g.fill(new Ellipse2D.Float(18.2f, 3.2f, 2.6f, 2.6f));
                    break;
                case RECT:
                    g.draw(new RoundRectangle2D.Float(4f, 5.5f, 16f, 13f, 2f, 2f));
                    break;
                case ELLIPSE:
                    g.draw(new Ellipse2D.Float(4f, 6f, 16f, 12f));
                    break;
                case TRIANGLE:
                    g.draw(polygon(new double[]{12, 20, 4}, new double[]{4, 19, 19}));
                    break;
                case POLYGON:
                    g.draw(regularPolygon(12, 12.5, 9.2, 5, -Math.PI / 2));
                    break;
                case TEXT:
                    g.draw(new Line2D.Float(5f, 6.5f, 19f, 6.5f));
                    g.draw(new Line2D.Float(12f, 6.5f, 12f, 19.5f));
                    g.draw(new Line2D.Float(8.5f, 19.5f, 15.5f, 19.5f));
                    break;
                case ERASER:
                    eraser(g, color);
                    break;
                case UNDO:
                    undo(g, false);
                    break;
                case REDO:
                    undo(g, true);
                    break;
                case SAVE:
                    save(g);
                    break;
                case OPEN:
                    open(g);
                    break;
                case NEW:
                    newFile(g);
                    break;
                case EXPORT:
                    export(g);
                    break;
                case PLAY:
                    g.fill(polygon(new double[]{8.5, 19.5, 8.5}, new double[]{5, 12, 19}));
                    break;
                case PAUSE:
                    g.fill(new RoundRectangle2D.Float(7.5f, 5f, 3.5f, 14f, 1.5f, 1.5f));
                    g.fill(new RoundRectangle2D.Float(13f, 5f, 3.5f, 14f, 1.5f, 1.5f));
                    break;
                case PREV:
                    g.fill(new Rectangle2D.Float(5.5f, 5.5f, 2.2f, 13f));
                    g.fill(polygon(new double[]{18, 8, 18}, new double[]{5, 12, 19}));
                    break;
                case NEXT:
                    g.fill(new Rectangle2D.Float(16.3f, 5.5f, 2.2f, 13f));
                    g.fill(polygon(new double[]{6, 16, 6}, new double[]{5, 12, 19}));
                    break;
                case LOOP:
                    loop(g);
                    break;
                case MUSIC:
                    g.draw(new Line2D.Float(9f, 17f, 9f, 5f));
                    g.draw(new Line2D.Float(9f, 5f, 19f, 3.5f));
                    g.draw(new Line2D.Float(19f, 3.5f, 19f, 15f));
                    g.fill(new Ellipse2D.Float(5.2f, 15.5f, 4.5f, 3.5f));
                    g.fill(new Ellipse2D.Float(15.2f, 13.5f, 4.5f, 3.5f));
                    break;
                case TRASH:
                    g.draw(new Line2D.Float(4.5f, 7f, 19.5f, 7f));
                    g.draw(new RoundRectangle2D.Float(9f, 3.5f, 6f, 3.5f, 1f, 1f));
                    Path2D can = new Path2D.Float();
                    can.moveTo(6.5f, 7f);
                    can.lineTo(7.5f, 20.5f);
                    can.lineTo(16.5f, 20.5f);
                    can.lineTo(17.5f, 7f);
                    g.draw(can);
                    g.draw(new Line2D.Float(10f, 10f, 10f, 17.5f));
                    g.draw(new Line2D.Float(14f, 10f, 14f, 17.5f));
                    break;
                case CHECK:
                    g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Line2D.Float(4.5f, 12.5f, 9.5f, 17.5f));
                    g.draw(new Line2D.Float(9.5f, 17.5f, 19.5f, 6.5f));
                    break;
                default:
                    break;
            }
            g.dispose();
        }

        private void pencil(Graphics2D g, Color color) {
            Path2D body = new Path2D.Float();
            body.moveTo(4f, 20f);
            body.lineTo(5.4f, 14.5f);
            body.lineTo(15.4f, 4.5f);
            body.quadTo(16.8f, 3.1f, 18.2f, 4.5f);
            body.lineTo(19.5f, 5.8f);
            body.quadTo(20.9f, 7.2f, 19.5f, 8.6f);
            body.lineTo(9.5f, 18.6f);
            body.closePath();
            g.draw(body);
            g.draw(new Line2D.Float(5.4f, 14.5f, 9.5f, 18.6f));
            g.fill(polygon(new double[]{4f, 5.1f, 8.4f}, new double[]{20f, 15.4f, 19.9f}));
        }

        private void eraser(Graphics2D g, Color color) {
            AffineTransform old = g.getTransform();
            g.rotate(-Math.PI / 4, 12, 12);
            g.draw(new RoundRectangle2D.Float(4.5f, 8f, 15f, 8.5f, 2.2f, 2.2f));
            g.draw(new Line2D.Float(10.5f, 8f, 10.5f, 16.5f));
            g.setColor(Theme.alpha(color, 42));
            g.fill(new RoundRectangle2D.Float(10.5f, 8f, 9f, 8.5f, 1.2f, 1.2f));
            g.setTransform(old);
        }

        private void undo(Graphics2D g, boolean mirrored) {
            AffineTransform old = g.getTransform();
            if (mirrored) {
                g.translate(24, 0);
                g.scale(-1, 1);
            }
            Path2D path = new Path2D.Float();
            path.moveTo(8.5f, 5.5f);
            path.lineTo(4.5f, 9.5f);
            path.lineTo(8.5f, 13.5f);
            g.draw(path);
            Path2D curve = new Path2D.Float();
            curve.moveTo(4.5f, 9.5f);
            curve.lineTo(14f, 9.5f);
            curve.quadTo(20f, 9.5f, 20f, 15f);
            curve.quadTo(20f, 20f, 14f, 20f);
            curve.lineTo(10f, 20f);
            g.draw(curve);
            g.setTransform(old);
        }

        private void save(Graphics2D g) {
            g.draw(new RoundRectangle2D.Float(4f, 3.5f, 16f, 17f, 2.5f, 2.5f));
            g.draw(new Rectangle2D.Float(8f, 3.5f, 8f, 5.5f));
            g.draw(new Rectangle2D.Float(7.5f, 13f, 9f, 7.5f));
            g.draw(new Line2D.Float(9.5f, 6.5f, 13.5f, 6.5f));
        }

        private void open(Graphics2D g) {
            Path2D folder = new Path2D.Float();
            folder.moveTo(3.5f, 20f);
            folder.lineTo(3.5f, 6f);
            folder.lineTo(9.5f, 6f);
            folder.lineTo(11.5f, 8.5f);
            folder.lineTo(20.5f, 8.5f);
            folder.lineTo(20.5f, 20f);
            folder.closePath();
            g.draw(folder);
            g.draw(new Line2D.Float(4f, 11f, 20f, 11f));
        }

        private void newFile(Graphics2D g) {
            Path2D page = new Path2D.Float();
            page.moveTo(6f, 3f);
            page.lineTo(14f, 3f);
            page.lineTo(19f, 8f);
            page.lineTo(19f, 21f);
            page.lineTo(6f, 21f);
            page.closePath();
            g.draw(page);
            g.draw(new Line2D.Float(14f, 3f, 14f, 8f));
            g.draw(new Line2D.Float(14f, 8f, 19f, 8f));
            g.draw(new Line2D.Float(9f, 13.5f, 16f, 13.5f));
            g.draw(new Line2D.Float(12.5f, 10f, 12.5f, 17f));
        }

        private void export(Graphics2D g) {
            g.draw(new RoundRectangle2D.Float(3f, 7f, 14f, 13f, 2f, 2f));
            Path2D mountain = new Path2D.Float();
            mountain.moveTo(5f, 17f);
            mountain.lineTo(9f, 12f);
            mountain.lineTo(12f, 15f);
            mountain.lineTo(14.5f, 12.5f);
            g.draw(mountain);
            g.fill(new Ellipse2D.Float(6.5f, 9.5f, 2f, 2f));
            g.draw(new Line2D.Float(16.5f, 9f, 21f, 4.5f));
            g.draw(new Line2D.Float(17f, 4.5f, 21f, 4.5f));
            g.draw(new Line2D.Float(21f, 4.5f, 21f, 8.5f));
        }

        private void loop(Graphics2D g) {
            Path2D arc1 = new Path2D.Float();
            arc1.moveTo(6f, 9f);
            arc1.curveTo(7.5f, 4.5f, 16.5f, 4f, 18.5f, 9f);
            g.draw(arc1);
            g.draw(new Line2D.Float(18.5f, 9f, 20.5f, 6.5f));
            g.draw(new Line2D.Float(18.5f, 9f, 15.5f, 8f));
            Path2D arc2 = new Path2D.Float();
            arc2.moveTo(18f, 15f);
            arc2.curveTo(16.5f, 19.5f, 7.5f, 20f, 5.5f, 15f);
            g.draw(arc2);
            g.draw(new Line2D.Float(5.5f, 15f, 3.5f, 17.5f));
            g.draw(new Line2D.Float(5.5f, 15f, 8.5f, 16f));
        }

        private Shape polygon(double[] xs, double[] ys) {
            Path2D path = new Path2D.Double();
            path.moveTo(xs[0], ys[0]);
            for (int i = 1; i < xs.length; i++) {
                path.lineTo(xs[i], ys[i]);
            }
            path.closePath();
            return path;
        }

        private Shape regularPolygon(double cx, double cy, double radius, int sides, double start) {
            Path2D path = new Path2D.Double();
            for (int i = 0; i < sides; i++) {
                double angle = start + i * 2 * Math.PI / sides;
                double px = cx + Math.cos(angle) * radius;
                double py = cy + Math.sin(angle) * radius;
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            return path;
        }
    }
}
