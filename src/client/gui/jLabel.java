package client.gui;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class jLabel extends JLabel {

    jLabel(ImageIcon img , JLabel oldImg){
       this.img = img ;
       this.oldImg = oldImg;
       new jLabel(img);
       setactions(this);
    }

   void  setactions(JLabel l) {
       l.addMouseListener(new MouseListener() {

           public void mouseClicked(MouseEvent ee) {
               JOptionPane.showMessageDialog(null, oldImg);

           }

           public void mouseEntered(MouseEvent ee) {

           }

           public void mouseExited(MouseEvent ee) {

           }

           public void mousePressed(MouseEvent ee) {

           }

           public void mouseReleased(MouseEvent ee) {

           }
       });

    }

    public jLabel(ImageIcon img) {
    }

    public ImageIcon getImg() {
        return img;
    }

    public void setImg(ImageIcon img) {
        this.img = img;
    }

    public JLabel getOldImg() {
        return oldImg;
    }

    public void setOldImg(JLabel oldImg) {
        this.oldImg = oldImg;
    }

    private ImageIcon img ;
    private JLabel oldImg;
}
