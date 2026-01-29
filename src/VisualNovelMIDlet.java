import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.Sprite; 
import javax.microedition.rms.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.util.Vector;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class VisualNovelMIDlet extends MIDlet {
    private Display display;
    private StartMenuCanvas menuCanvas;
    private VNEngineCanvas gameCanvas;
    private String gameTitle = "Visual Novel Engine";
    private String startBgName = "none";
    boolean onlyTextMode = false;

    public VisualNovelMIDlet() {
        display = Display.getDisplay(this);
        loadGameConfig();
    }

    protected void startApp() {
        showMenu();
    }

    private void loadGameConfig() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/story.txt");
            if (is != null) {
                java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                int b;
                while ((b = is.read()) != -1) os.write(b);
                String content = new String(os.toByteArray());
                
                int titleIdx = content.indexOf("TITLE:");
                if (titleIdx != -1) {
                    int endLine = content.indexOf('\n', titleIdx);
                    if (endLine == -1) endLine = content.length();
                    gameTitle = content.substring(titleIdx + 6, endLine).trim();
                }

                int bgIdx = content.indexOf("STBG:");
                if (bgIdx != -1) {
                    int endLine = content.indexOf('\n', bgIdx);
                    if (endLine == -1) endLine = content.length();
                    startBgName = content.substring(bgIdx + 5, endLine).trim();
                }
                is.close();
            }
        } catch (Exception e) {}
    }

    public String getGameTitle() { return gameTitle; }
    public String getStartBgName() { return startBgName; }

    public void showMenu() {
        if (menuCanvas == null) {
            menuCanvas = new StartMenuCanvas(this);
        }
        display.setCurrent(menuCanvas);
    }

    public void startNewGame() {
        gameCanvas = new VNEngineCanvas(this);
        display.setCurrent(gameCanvas);
        new Thread(gameCanvas).start();
    }

    public void continueGame() {
        gameCanvas = new VNEngineCanvas(this);
        display.setCurrent(gameCanvas);
        new Thread(gameCanvas).start();
        gameCanvas.loadGameState();
    }

    protected void pauseApp() {}

    protected void destroyApp(boolean unconditional) {
        if (gameCanvas != null) {
            gameCanvas.stop();
        }
    }

    public void exit() {
        destroyApp(true);
        notifyDestroyed();
    }

    protected byte[] fetchRawData(String path) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/" + path);
            if (is == null) return null;
            java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) os.write(b);
            is.close();
            return os.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}

class ImageUtils {
    public static Image createScaledImage(Image src, int dstW, int dstH) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        
        if (srcW == dstW && srcH == dstH) return src;

        int ratioW = (dstW * 1000) / srcW;
        int ratioH = (dstH * 1000) / srcH;
        
        int ratio = (ratioW > ratioH) ? ratioW : ratioH;
        
        int finalW = (srcW * ratio) / 1000;
        int finalH = (srcH * ratio) / 1000;

        Image tmp = Image.createImage(dstW, dstH);
        Graphics g = tmp.getGraphics();
        
        int offsetX = (dstW - finalW) / 2;
        int offsetY = (dstH - finalH) / 2;

        int[] rawInput = new int[srcW];
        int[] rawOutput = new int[finalW];

        Image scaledBuffer = Image.createImage(finalW, finalH);
        Graphics bg = scaledBuffer.getGraphics();

        for (int y = 0; y < finalH; y++) {
            int srcY = (y * srcH) / finalH;
            src.getRGB(rawInput, 0, srcW, 0, srcY, srcW, 1);
            for (int x = 0; x < finalW; x++) {
                int srcX = (x * srcW) / finalW;
                rawOutput[x] = rawInput[srcX];
            }
            bg.drawRGB(rawOutput, 0, finalW, 0, y, finalW, 1, true);
        }

        g.drawImage(scaledBuffer, offsetX, offsetY, Graphics.TOP | Graphics.LEFT);
        
        return tmp;
    }
}

class StartMenuCanvas extends Canvas {
    private VisualNovelMIDlet midlet;
    private String[] options = {"New Game", "Continue", "Exit"};
    private int selectedIndex = 0;
    private Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    private Font menuFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    private Image startBackground;

    public StartMenuCanvas(VisualNovelMIDlet midlet) {
        this.midlet = midlet;
        loadBackground();
    }

    private void loadBackground() {
        String bgName = midlet.getStartBgName();
        if (bgName != null && !bgName.toLowerCase().equals("none")) {
            byte[] data = midlet.fetchRawData(bgName);
            if (data != null) {
                try {
                    Image raw = Image.createImage(data, 0, data.length);
                    startBackground = ImageUtils.createScaledImage(raw, getWidth(), getHeight());
                } catch (Exception e) {}
            }
        }
    }

protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        if (startBackground != null) {
            g.drawImage(startBackground, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(10, 10, 20);
            g.fillRect(0, 0, w, h);
        }

        String title = midlet.getGameTitle();
        g.setFont(titleFont);
        int titleY = h / 5;
        
        g.setColor(0, 0, 0);
        drawWrappedTitle(g, title, w / 2 + 1, titleY + 1, w - 20);
        g.setColor(255, 255, 255);
        drawWrappedTitle(g, title, w / 2, titleY, w - 20);

        g.setFont(menuFont);
        int itemHeight = menuFont.getHeight() + 10;
        int totalMenuHeight = options.length * itemHeight;
        int startY = (h * 2/3) - (totalMenuHeight / 2);

        for (int i = 0; i < options.length; i++) {
            int itemY = startY + (i * itemHeight);
            int rectW = w * 3/4;
            int rectX = (w - rectW) / 2;

            if (i == selectedIndex) {
                g.setColor(50, 50, 150);
                g.fillRect(rectX, itemY - 2, rectW, itemHeight - 4);
                g.setColor(255, 255, 0);
            } else {
                g.setColor( 0, 0, 0 );
                g.fillRect(rectX + 10, itemY - 2, rectW - 20, itemHeight - 4);
                g.setColor(200, 200, 200);
            }
            g.drawString(options[i], w / 2, itemY, Graphics.TOP | Graphics.HCENTER);
        }
    }

private void drawWrappedTitle(Graphics g, String title, int x, int y, int maxW) {
        if (titleFont.stringWidth(title) > maxW) {
            int spaceIdx = title.lastIndexOf(' ', title.length() / 2 + 2);
            if (spaceIdx != -1) {
                g.drawString(title.substring(0, spaceIdx), x, y, Graphics.TOP | Graphics.HCENTER);
                g.drawString(title.substring(spaceIdx + 1), x, y + titleFont.getHeight(), Graphics.TOP | Graphics.HCENTER);
            } else {
                g.drawString(title, x, y, Graphics.TOP | Graphics.HCENTER);
            }
        } else {
            g.drawString(title, x, y, Graphics.TOP | Graphics.HCENTER);
        }
    }

    protected void pointerPressed(int x, int y) {
        int h = getHeight();
        int itemHeight = menuFont.getHeight() + 10;
        int totalMenuHeight = options.length * itemHeight;
        int startY = (h * 2/3) - (totalMenuHeight / 2);

        for (int i = 0; i < options.length; i++) {
            int itemY = startY + (i * itemHeight);
            if (y >= itemY - 2 && y <= itemY + itemHeight - 6) {
                if (selectedIndex == i) handleAction();
                else selectedIndex = i;
                repaint();
                return;
            }
        }
    }

protected void keyPressed(int keyCode) {
    int action = getGameAction(keyCode);
    
    if (action == Canvas.UP || keyCode == Canvas.KEY_NUM2) {
        selectedIndex = (selectedIndex - 1 + options.length) % options.length;
    }
    else if (action == Canvas.DOWN || keyCode == Canvas.KEY_NUM8) {
        selectedIndex = (selectedIndex + 1) % options.length;
    }
    else if (
        action == Canvas.FIRE || keyCode == -5 || keyCode == 10 || keyCode == -7 || keyCode == 53 || keyCode == Canvas.KEY_NUM5) {
        handleAction();
    }
    
    repaint();
}

    private void handleAction() {
        if (selectedIndex == 0) midlet.startNewGame();
        else if (selectedIndex == 1) midlet.continueGame();
        else if (selectedIndex == 2) midlet.exit();
    }
}

class VNEngineCanvas extends Canvas implements Runnable {
    private VisualNovelMIDlet midlet;
    private String currentSpeaker = "";
    private String currentText = "";
    private Vector wrappedLines = new Vector();
    private Image background;
    private String currentBgName = "none";
    private Image character, character2;
    private String currentCharName = "none", currentChar2Name = "none";
    private int charXPosition = 0, char2XPosition = 0, charRotation = 0, char2Rotation = 0;
    private boolean flipH = false, flipV = false, flipH2 = false, flipV2 = false;
    
    private boolean choiceMode = false, menuMode = false;
    private String[] menuOptions = {"Resume", "Save Game", "Load Game", "Main Menu", "Exit"};
    private int selectedMenuIndex = 0;

    private Vector choiceTexts = new Vector(), choiceTargets = new Vector();
    private int selectedChoiceIndex = -1;
    private boolean autoMode = false;
    private long waitTime = 0;
    private volatile boolean running = true;
    private String[] scriptLines;
    private int scriptIndex = 0;

    private Player musicPlayer;
    private String currentMusicName = "none";
    private String musicLog = "Music: Stopped";
    
    private final int TEXT_BOX_HEIGHT = 100;
    private final int PADDING = 10;
    
    private Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    public VNEngineCanvas(VisualNovelMIDlet midlet) {
        this.midlet = midlet;
        loadScript("story.txt");
        advanceScript();
    }

    private void loadScript(String path) {
        byte[] data = midlet.fetchRawData(path);
        if (data != null) {
            String content = new String(data);
            scriptLines = split(content, '\n');
        } else {
            currentText = "Error: Script file 'story.txt' not found.";
            wrapText(currentText);
        }
    }

    private String clean(String s) {
        if (s == null) return "";
        s = s.trim();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c <= 126) sb.append(c);
        }
        return sb.toString().trim();
    }

    private synchronized void advanceScript() {
        if (scriptLines == null || scriptIndex >= scriptLines.length || choiceMode || menuMode) return;
        while (scriptIndex < scriptLines.length) {
            String line = clean(scriptLines[scriptIndex++]);
            if (line.length() == 0 || line.startsWith("#")) continue;
            if (processLine(line)) break;
        }
        repaint();
    }

    private boolean processLine(String line) {
        if (line.startsWith("BG:")) {
            currentBgName = line.substring(3).trim();
            background = loadImage(currentBgName, true);
        } else if (line.startsWith("CH:")) {
            currentCharName = line.substring(3).trim();
            character = loadImage(currentCharName, false);
        } else if (line.startsWith("CH2:")) {
            currentChar2Name = line.substring(4).trim();
            character2 = loadImage(currentChar2Name, false);
        } else if (line.startsWith("POS:")) {
            charXPosition = parsePosition(line.substring(4).trim());
        } else if (line.startsWith("POS2:")) {
            char2XPosition = parsePosition(line.substring(5).trim());
        } else if (line.startsWith("ROT:")) {
            charRotation = parseRotation(line.substring(4).trim());
        } else if (line.startsWith("ROT2:")) {
            char2Rotation = parseRotation(line.substring(5).trim());
        } else if (line.startsWith("FLIP:")) {
            String val = line.substring(5).trim().toUpperCase();
            flipH = val.indexOf("H") != -1;
            flipV = val.indexOf("V") != -1;
        } else if (line.startsWith("FLIP2:")) {
            String val = line.substring(6).trim().toUpperCase();
            flipH2 = val.indexOf("H") != -1;
            flipV2 = val.indexOf("V") != -1;
        } else if (line.startsWith("MUS:")) {
            playMusic(line.substring(4).trim());
        } else if (line.startsWith("STOPMUS:")) {
            stopMusic();
        } else if (line.startsWith("NAME:")) {
            currentSpeaker = line.substring(5).trim();
        } else if (line.startsWith("AUTO:")) {
            autoMode = line.substring(5).trim().toUpperCase().equals("ON");
        } else if (line.startsWith("GOTO:")) {
            jumpToLabel(line.substring(5).trim());
        } else if (line.startsWith("CHOICE:")) {
            setupChoice(line.substring(7).trim());
            return true;
        } else if (line.startsWith("ONLYTXT:")) {
            currentText = line.substring(4).trim();
            wrapText(currentText);
            midlet.onlyTextMode = true;
            return true;
        } else if (line.startsWith("TXT:")) {
            currentText = line.substring(4).trim();
            wrapText(currentText);
            midlet.onlyTextMode = false;
            return true;
        } else if (line.startsWith("WAIT:")) {
            try {
            waitTime = Long.parseLong(line.substring(5).trim());
        } catch (Exception e) {
            waitTime = 0;
        }
            return true;
        }
        return false;
    }

        private void playMusic(String musicFile) {
        if (musicFile == null || musicFile.toLowerCase().equals("none")) {
            stopMusic();
            return;
        }
        if (musicFile.equals(currentMusicName)) return;

        stopMusic();
        musicLog = "Attempting Stream: " + musicFile;

        InputStream is = getClass().getResourceAsStream("/" + musicFile);
        if (is == null) {
            musicLog = "Err: File not found: " + musicFile;
            return;
        }

        try {
            String type = "audio/midi";
            String ext = musicFile.toLowerCase();
            if (ext.endsWith(".wav")) type = "audio/x-wav";
            else if (ext.endsWith(".mp3")) type = "audio/mpeg";
            else if (ext.endsWith(".amr")) type = "audio/amr";

            musicPlayer = Manager.createPlayer(is, type);
            musicPlayer.realize();
            
            VolumeControl vc = (VolumeControl) musicPlayer.getControl("VolumeControl");
            if (vc != null) vc.setLevel(100);

            musicPlayer.prefetch();
            musicPlayer.setLoopCount(-1); 
            musicPlayer.start();
            currentMusicName = musicFile;
            musicLog = "Playing: " + musicFile;
        } catch (Exception e) {
            try {
                if (is != null) try { is.close(); } catch(Exception ex2){}
                is = getClass().getResourceAsStream("/" + musicFile);
                musicPlayer = Manager.createPlayer(is, null);
                musicPlayer.realize();
                musicPlayer.prefetch();
                musicPlayer.start();
                currentMusicName = musicFile;
                musicLog = "Playing (Auto): " + musicFile;
            } catch (Exception ex) {
                currentMusicName = "none";
                musicLog = "Err OOM/Format: " + ex.getMessage();
                try { if (is != null) is.close(); } catch(Exception ex2){}
            }
        }
        repaint();
    }

    private void stopMusic() {
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
                musicPlayer.deallocate();
                musicPlayer.close();
            } catch (Exception e) {}
            musicPlayer = null;
            currentMusicName = "none";
            musicLog = "Music: Stopped";
        }
    }

    public void saveGameState() {
        try {
            RecordStore rs = RecordStore.openRecordStore("VNSaveData", true);
            int saveIdx = scriptIndex - 1;
            String state = saveIdx + ";" + currentBgName + ";" + currentCharName + ";" + currentSpeaker + ";" + currentMusicName;
            byte[] data = state.getBytes();
            if (rs.getNumRecords() > 0) rs.setRecord(1, data, 0, data.length);
            else rs.addRecord(data, 0, data.length);
            rs.closeRecordStore();
        } catch (Exception e) {}
    }

public void loadGameState() {
        try {
            RecordStore rs = RecordStore.openRecordStore("VNSaveData", false);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                String[] parts = split(new String(data), ';');
                
                scriptIndex = Integer.parseInt(parts[0]);
                currentBgName = parts[1];
                currentCharName = parts[2];
                currentSpeaker = parts[3];
                
                background = loadImage(currentBgName, true);
                character = loadImage(currentCharName, false);
                
                if (parts.length > 4) playMusic(parts[4]);

                choiceMode = false;
                menuMode = false;
                
                if (scriptIndex > 0) {
                    scriptIndex--; 
                }
                
                advanceScript(); 
                
            }
            rs.closeRecordStore();
        } catch (Exception e) {
        }
        repaint();
    }

    private void setupChoice(String data) {
        choiceTexts.removeAllElements();
        choiceTargets.removeAllElements();
        String[] parts = split(data, ',');
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            int pipeIdx = part.indexOf('|');
            if (pipeIdx != -1) {
                choiceTexts.addElement(part.substring(0, pipeIdx).trim());
                choiceTargets.addElement(part.substring(pipeIdx + 1).trim());
            }
        }
        if (choiceTexts.size() > 0) {
            choiceMode = true;
            selectedChoiceIndex = -1;
        }
    }

    private void jumpToLabel(String labelName) {
        String target = clean(labelName).toUpperCase();
        for (int i = 0; i < scriptLines.length; i++) {
            String cur = clean(scriptLines[i]).toUpperCase();
            if (cur.startsWith("LABEL:") && cur.substring(6).trim().equals(target)) {
                scriptIndex = i + 1;
                choiceMode = false;
                return;
            }
        }
    }

    private int parsePosition(String pos) {
        pos = pos.toUpperCase();
        if (pos.equals("LEFT")) return 3;
        if (pos.equals("RIGHT")) return 4;
        if (pos.equals("FAR_LEFT")) return 1;
        if (pos.equals("FAR_RIGHT")) return 2;
        return 0; 
    }

    private int parseRotation(String val) {
        try {
            int deg = Integer.parseInt(val);
            if (deg >= 45 && deg < 135) return 90;
            if (deg >= 135 && deg < 225) return 180;
            if (deg >= 225 && deg < 315) return 270;
        } catch (Exception e) {}
        return 0;
    }

    private Image loadImage(String name, boolean scaleToScreen) {
        if (name == null || name.toLowerCase().equals("none") || name.length() == 0) return null;
        byte[] data = midlet.fetchRawData(name);
        try { 
            if (data == null) return null;
            Image img = Image.createImage(data, 0, data.length);
            if (scaleToScreen) {
                return ImageUtils.createScaledImage(img, getWidth(), getHeight());
            }
            return img;
        } catch (Exception e) { return null; }
    }

    private void wrapText(String text) {
        wrappedLines.removeAllElements();
        if (text == null || text.length() == 0) return;
        int maxWidth = getWidth() - (PADDING * 2);
        String[] words = split(text, ' ');
        StringBuffer currentLine = new StringBuffer();
        for (int i = 0; i < words.length; i++) {
            String trial = (currentLine.length() == 0) ? words[i] : currentLine.toString() + " " + words[i];
            if (font.stringWidth(trial) <= maxWidth) {
                if (currentLine.length() != 0) currentLine.append(" ");
                currentLine.append(words[i]);
            } else {
                wrappedLines.addElement(currentLine.toString());
                currentLine = new StringBuffer(words[i]);
            }
        }
        if (currentLine.length() > 0) wrappedLines.addElement(currentLine.toString());
    }

protected void paint(Graphics g) {
        int w = getWidth(), h = getHeight();
        
        if (background != null) {
            g.drawImage(background, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else { 
            g.setColor(0, 0, 0); 
            g.fillRect(0, 0, w, h); 
        }
        
        if (character != null) renderChar(g, character, charXPosition, charRotation, flipH, flipV, w, h);
        if (character2 != null) renderChar(g, character2, char2XPosition, char2Rotation, flipH2, flipV2, w, h);
        
        if (!midlet.onlyTextMode && !menuMode) {
            g.setColor(20, 20, 30); 
            g.fillRect(0, h - TEXT_BOX_HEIGHT, w, TEXT_BOX_HEIGHT);
            g.setColor(150, 150, 200); 
            g.drawRect(2, h - TEXT_BOX_HEIGHT + 2, w - 5, TEXT_BOX_HEIGHT - 5);
        }
        
        if (menuMode) {
            renderMenu(g, w, h);
        } else if (choiceMode) {
            renderChoices(g, w, h);
        } else {
            renderDialogue(g, w, h);
        }
    }

    private void renderMenu(Graphics g, int w, int h) {
        g.setColor(0, 0, 0); g.fillRect(0, 0, w, h);
        int menuW = w - 60, menuH = menuOptions.length * 35 + 20;
        int menuX = (w - menuW) / 2, menuY = (h - menuH) / 2;
        g.setColor(30, 30, 50); g.fillRect(menuX, menuY, menuW, menuH);
        g.setColor(255, 255, 255); g.drawRect(menuX, menuY, menuW, menuH);
        for (int i = 0; i < menuOptions.length; i++) {
            if (i == selectedMenuIndex) {
                g.setColor(100, 100, 255); g.fillRect(menuX + 5, menuY + 10 + (i * 35), menuW - 10, 30);
                g.setColor(255, 255, 0);
            } else g.setColor(255, 255, 255);
            g.drawString(menuOptions[i], menuX + menuW / 2, menuY + 15 + (i * 35), Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void renderChoices(Graphics g, int w, int h) {
        g.setFont(font); g.setColor(255, 255, 255);
        g.drawString("Select an option:", PADDING, h - TEXT_BOX_HEIGHT + PADDING, 0);
        for (int i = 0; i < choiceTexts.size(); i++) {
            int itemY = h - TEXT_BOX_HEIGHT + 30 + (i * 20);
            if (i == selectedChoiceIndex) {
                g.setColor(60, 60, 180); g.fillRect(PADDING, itemY, w - (PADDING * 2), 18);
                g.setColor(255, 255, 0);
            } else g.setColor(200, 200, 200);
            g.drawString("> " + (String)choiceTexts.elementAt(i), PADDING + 4, itemY + 2, 0);
        }
    }

    private void renderDialogue(Graphics g, int w, int h) {
        Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    Font activeFont = midlet.onlyTextMode ? boldFont : font;
    g.setFont(activeFont);
    
    int currentY = h - TEXT_BOX_HEIGHT + PADDING;


    if (currentSpeaker.length() > 0) {
        if (midlet.onlyTextMode) {
            g.setColor(255, 255, 255);
            g.drawString(currentSpeaker + ":", PADDING + 1, currentY + 1, 0);
            g.setColor(0, 0, 0);
        } else {
            g.setColor(255, 255, 0);
        }
        g.drawString(currentSpeaker + ":", PADDING, currentY, 0);
        currentY += activeFont.getHeight() + 4;
    }

    for (int i = 0; i < wrappedLines.size(); i++) {
        String line = (String) wrappedLines.elementAt(i);
        
        if (midlet.onlyTextMode) {
            g.setColor(255, 255, 255);
            g.drawString(line, PADDING + 1, currentY + 1, 0);
            g.setColor(0, 0, 0);
        } else {
            g.setColor(255, 255, 255);
        }
        
        g.drawString(line, PADDING, currentY, 0);
        currentY += activeFont.getHeight() + 2;
    }
}

    private void renderChar(Graphics g, Image img, int posX, int rot, boolean fh, boolean fv, int w, int h) {
    int drawX = w / 2;
    if (posX == 3) drawX = 0;
    else if (posX == 4) drawX = w;
    else if (posX == 1) drawX = w / 4;
    else if (posX == 2) drawX = (w * 3) / 4;

    Sprite s = new Sprite(img);
        int trans = Sprite.TRANS_NONE;
        if (fh && fv) trans = Sprite.TRANS_ROT180;
        else if (fh) trans = Sprite.TRANS_MIRROR;
        else if (fv) trans = Sprite.TRANS_MIRROR_ROT180;
        if (rot == 90) trans = fh ? Sprite.TRANS_MIRROR_ROT90 : Sprite.TRANS_ROT90;
        else if (rot == 180) trans = fh ? Sprite.TRANS_MIRROR_ROT180 : Sprite.TRANS_ROT180;
        else if (rot == 270) trans = fh ? Sprite.TRANS_MIRROR_ROT270 : Sprite.TRANS_ROT270;
        
        s.setTransform(trans);
    int sx = drawX;
    if (posX == 4) sx -= s.getWidth();
    else if (posX == 0 || posX == 1 || posX == 2) sx -= s.getWidth()/2;

    s.setPosition(sx, h - s.getHeight()); 
    s.paint(g);
}
    
    protected void pointerPressed(int x, int y) {
        int w = getWidth(), h = getHeight();
        if (menuMode) {
            int menuW = w - 60, menuH = menuOptions.length * 35 + 20;
            int menuX = (w - menuW) / 2, menuY = (h - menuH) / 2;
            for (int i = 0; i < menuOptions.length; i++) {
                int itemY = menuY + 10 + (i * 35);
                if (x >= menuX && x <= menuX + menuW && y >= itemY && y <= itemY + 30) {
                    if (selectedMenuIndex == i) handleMenuAction();
                    else selectedMenuIndex = i;
                    repaint(); return;
                }
            }
        } else if (choiceMode) {
            for (int i = 0; i < choiceTexts.size(); i++) {
                int itemY = h - TEXT_BOX_HEIGHT + 30 + (i * 20);
                if (y >= itemY && y <= itemY + 18) {
                    if (selectedChoiceIndex == i) {
                        String target = (String)choiceTargets.elementAt(selectedChoiceIndex);
                        choiceMode = false; jumpToLabel(target); advanceScript();
                    } else selectedChoiceIndex = i;
                    repaint(); return;
                }
            }
        } else advanceScript();
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (keyCode == -11 || keyCode == -7 || keyCode == 48) { menuMode = !menuMode; repaint(); return; }
        if (menuMode) {
            if (action == Canvas.UP || keyCode == 50) selectedMenuIndex = (selectedMenuIndex - 1 + menuOptions.length) % menuOptions.length;
            else if (action == Canvas.DOWN || keyCode == 56) selectedMenuIndex = (selectedMenuIndex + 1) % menuOptions.length;
            else if (action == Canvas.FIRE || keyCode == -5 || keyCode == 10 || keyCode == -7 || keyCode == 53 || keyCode == Canvas.KEY_NUM5) handleMenuAction();
        } else if (choiceMode) {
            if (action == Canvas.UP || keyCode == 50) selectedChoiceIndex = (selectedChoiceIndex - 1 + choiceTexts.size()) % choiceTexts.size();
            else if (action == Canvas.DOWN || keyCode == 56) selectedChoiceIndex = (selectedChoiceIndex + 1) % choiceTexts.size();
            else if (action == Canvas.FIRE || keyCode == -5 || keyCode == 10 || keyCode == -7 || keyCode == 53 || keyCode == Canvas.KEY_NUM5) {
                if (selectedChoiceIndex != -1) {
                    String target = (String)choiceTargets.elementAt(selectedChoiceIndex);
                    choiceMode = false; jumpToLabel(target); advanceScript();
                }
            }
        } else advanceScript();
        repaint();
    }
    
    private void handleMenuAction() {
        switch (selectedMenuIndex) {
            case 0: menuMode = false; break;
            case 1: saveGameState(); menuMode = false; break;
            case 2: loadGameState(); menuMode = false; break;
            case 3: stopMusic(); running = false; midlet.showMenu(); break;
            case 4: stopMusic(); midlet.exit(); break;
        }
    }

    private String[] split(String str, char delimiter) {
        Vector nodes = new Vector();
        int index = str.indexOf(delimiter);
        while (index >= 0) {
            nodes.addElement(str.substring(0, index));
            str = str.substring(index + 1);
            index = str.indexOf(delimiter);
        }
        nodes.addElement(str);
        String[] result = new String[nodes.size()];
        nodes.copyInto(result);
        return result;
    }

    public void stop() { stopMusic(); running = false; }

public void run() {
    while (running) {
        if (!menuMode && !choiceMode) {
            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                    waitTime = 0;
                    advanceScript();
                } catch (InterruptedException e) {}
            } else if (autoMode) {
                try {
                    Thread.sleep(2000);
                    advanceScript();
                } catch (InterruptedException e) {}
            }
        }
        
        try { Thread.sleep(50); } catch (InterruptedException e) {}
    }
}
}