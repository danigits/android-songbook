package igrek.songbook.graphics.canvas;

import android.content.Context;

import java.util.List;

import igrek.songbook.graphics.gui.GUIListener;
import igrek.songbook.logic.crdfile.CRDFragment;
import igrek.songbook.logic.crdfile.CRDLine;
import igrek.songbook.logic.crdfile.CRDModel;
import igrek.songbook.logic.crdfile.CRDTextType;
import igrek.songbook.settings.Config;

public class CanvasGraphics extends BaseCanvasGraphics {

    private CRDModel crdModel = null;

    private float scroll = 0;
    private float startScroll = 0;

    public CanvasGraphics(Context context, GUIListener guiListener) {
        super(context, guiListener);
    }

    //TODO: blokowanie przewijania w dół, jeśli skończył się już plik

    //TODO: obsługa gestów
    //TODO: zmiana rozmiaru czcionki
    //TODO: autoscroll + gesty

    public void setCRDModel(CRDModel crdModel) {
        this.crdModel = crdModel;
        repaint();
    }

    @Override
    public void init() {
        setFontSize(Config.Fonts.fontsize);
        guiListener.onGraphicsInitialized(w, h, paint);
    }

    @Override
    public void onRepaint() {

        drawBackground();

        drawFileContent();
    }

    private void drawFileContent() {
        setFontSize(Config.Fonts.fontsize);

        setColor(0xffffff);

        if (crdModel != null) {
            for (CRDLine line : crdModel.getLines()) {
                drawTextLine(line, scroll, Config.Fonts.lineheight);
            }
        }
    }

    private void drawBackground() {
        setColor(Config.Colors.background);
        clearScreen();
    }

    private void drawTextLine(CRDLine line, float scroll, float lineheight) {
        float y = line.getY() - scroll;
        if (y > h) return;
        if (y + lineheight < 0) return;

        for (CRDFragment fragment : line.getFragments()) {

            if (fragment.getType() == CRDTextType.REGULAR_TEXT) {
                setColor(0xffffff);
            } else if (fragment.getType() == CRDTextType.CHORDS) {
                setColor(0xf00000);
            }

            drawTextUnaligned(fragment.getText(), fragment.getX(), y + lineheight);
        }
    }

    @Override
    protected void onTouchDown(float touchX, float touchY) {
        super.onTouchDown(touchX, touchY);
        startScroll = scroll;
    }

    @Override
    protected void onTouchMove(float touchX, float touchY) {
        scroll = startScroll + startTouchY - touchY;
        if (scroll < 0) scroll = 0; //za duże przeskrolowanie w górę
        float bottomY = getTextBottomY();
        float reserve = 0.125f * h;
        if(bottomY > h){
            if(bottomY + reserve - scroll < h) { //za duże przescrollowanie w dół
                scroll = bottomY + reserve - h;
            }
        }else{
            //brak możliwości scrollowania
            scroll = 0;
        }
        repaint();
    }

    @Override
    protected void onTouchUp(float touchX, float touchY) {
        float deltaX = touchX - startTouchX;
        float deltaY = touchY - startTouchY;
        if(Math.abs(deltaX) > Math.abs(deltaY)){
            if(Math.abs(deltaX) >= 0.4 * w){
                if(deltaX < 0){
                    guiListener.onTransposed(-1);
                }else if(deltaX > 0){
                    guiListener.onTransposed(+1);
                }
            }
        }
    }

    private float getTextBottomY() {
        if (crdModel == null) return 0;
        List<CRDLine> lines = crdModel.getLines();
        if (lines == null || lines.isEmpty()) return 0;
        CRDLine lastLine = lines.get(lines.size() - 1);
        if (lastLine == null) return 0;
        return lastLine.getY() + Config.Fonts.lineheight;
    }
}
