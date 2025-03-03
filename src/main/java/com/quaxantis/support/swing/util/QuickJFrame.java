package com.quaxantis.support.swing.util;

import javax.swing.JFrame;
import java.awt.*;
import java.util.function.Consumer;

public class QuickJFrame<C extends Component> {

    private final JFrame jFrame;
    private C component;
    private Consumer<C> onDispose = _ -> {};

    private QuickJFrame(C component) {
        this.component = component;
        this.jFrame = new JFrame(){
            @Override
            public void dispose() {
                onDispose.accept(component);
                super.dispose();
            }
        };
        this.jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.jFrame.setLayout(new GridLayout(1,1));
        this.jFrame.add(this.component);
//        this.jFrame.setContentPane((Container) this.component);
    }


    public static <C extends Component> QuickJFrame<C> of(C component) {
        return new QuickJFrame<>(component);
    }

    public QuickJFrame<C> withTitle(String title){
        jFrame.setTitle(title);
        return this;
    }

    public QuickJFrame<C> onDispose(Consumer<C> action) {
        this.onDispose = this.onDispose.andThen(action);
        return this;
    }

    public void show() {
        jFrame.pack();
        jFrame.setMinimumSize(new Dimension(640, 480));
        jFrame.setPreferredSize(component.getPreferredSize());
        jFrame.setVisible(true);
    }



}
