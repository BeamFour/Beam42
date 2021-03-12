package org.redukti.jfotoptix.io;

import org.redukti.jfotoptix.math.Vector2;
import org.redukti.jfotoptix.math.Vector2Pair;

import static org.redukti.jfotoptix.io.Renderer.Style.StyleBackground;

/**
 SVG file rendering driver

 This class implements a SVG graphic output driver.
 */
public class RendererSvg extends Renderer2d {

     StringBuilder _out = new StringBuilder();
    /** Create a new svg renderer with given resolution. The
     @ref write function must be used to write svg to output
     stream. */
    RendererSvg (double width, double height,
               Rgb bg) {
         _2d_output_res = new Vector2(width, height);

         _styles_color[StyleBackground.value] = bg;
         _styles_color[Style.StyleForeground.value] = bg.negate();

         clear ();
    }

     void svg_begin_rect (double x1, double y1, double x2, double y2,
                                  boolean terminate)
     {
          _out.append("<rect ")
                  .append( "x=\"").append(x1).append("\" ")
                  .append("y=\"").append(y1).append("\" ")
                  .append("width=\"").append(x2 - x1).append("\" ")
                  .append("height=\"").append(y2 - y1).append("\" ");

          if (terminate)
               _out.append(" />").append(System.lineSeparator());
     }

     void svg_begin_line (double x1, double y1, double x2, double y2,
                                  boolean terminate)
     {
          _out.append("<line ")
                  .append("x1=\"").append(x1).append("\" ")
                  .append("y1=\"").append(y1).append("\" ")
                  .append("x2=\"").append(x2).append("\" ")
                  .append("y2=\"").append(y2) .append("\" ");

          if (terminate)
               _out.append(" />").append(System.lineSeparator());
     }

     StringBuilder write_srgb (Rgb rgb)
     {
          _out.append(String.format("#%02x%02x%02x", (int)(rgb.r * 255.0),
             (int)(rgb.g * 255.0), (int)(rgb.b * 255.0)));
          return _out;
     }

     void svg_add_fill (Rgb rgb)
     {
          _out.append(" fill=\"");
          write_srgb (rgb);
          _out.append("\"");
     }

     void svg_end ()
     {
          _out.append(" />").append(System.lineSeparator());
     }

     void clear ()
     {
          _out.setLength(0);

          // background
          svg_begin_rect (0.0, 0.0, _2d_output_res.x (), _2d_output_res.y (), false);
          svg_add_fill (get_style_color (StyleBackground));
          svg_end ();

          _out.append("<defs>").append(System.lineSeparator());

          // dot shaped point
          _out.append("<g id=\"")
                  .append("dot")
                  .append("\">").append(System.lineSeparator());
          svg_begin_line (1, 1, 0, 0, true);
//          _out << "</g>" << std::endl;
//
//          // cross shaped point
//          _out << "<g id=\""
//                  << "cross"
//                  << "\">" << std::endl;
//          svg_begin_line (-3, 0, 3, 0, true);
//          svg_begin_line (0, -3, 0, 3, true);
//          _out << "</g>" << std::endl;
//
//          // square shaped point
//          _out << "<g id=\""
//                  << "square"
//                  << "\">" << std::endl;
//          svg_begin_line (-3, -3, -3, 3, true);
//          svg_begin_line (-3, 3, 3, 3, true);
//          svg_begin_line (3, 3, 3, -3, true);
//          svg_begin_line (3, -3, -3, -3, true);
//          _out << "</g>" << std::endl;
//
//          // round shaped point
//          _out << "<g id=\""
//                  << "round"
//                  << "\">" << std::endl;
//          svg_begin_ellipse (0, 0, 3, 3, false);
//          _out << " fill=\"none\" />";
//          _out << "</g>" << std::endl;
//
//          // triangle shaped point
//          _out << "<g id=\""
//                  << "triangle"
//                  << "\">" << std::endl;
//          svg_begin_line (0, -3, -3, 3, true);
//          svg_begin_line (-3, 3, 3, 3, true);
//          svg_begin_line (0, -3, +3, +3, true);
//          _out << "</g>" << std::endl;
//
//          _out << "</defs>" << std::endl;
     }

     @Override
     public void draw_point(Vector2 p, Rgb rgb, PointStyle s) {

     }

     @Override
     public void draw_segment(Vector2Pair s, Rgb rgb) {

     }
}
