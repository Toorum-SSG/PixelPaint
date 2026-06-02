# 🎨 PaintApp

Jednoduchá desktopová kreslící aplikace napsaná v čistém Javě (Swing).  
Žádné externí knihovny – stačí JDK 11 nebo novější.

---

## ▶️ Spuštění

```bash
java -jar PaintApp.jar
```


---

## 🖱️ Ovládání

| Akce | Způsob |
|------|--------|
| Kreslit | Levé tlačítko myši |
| Kreslit sekundární barvou | Pravé tlačítko myši |
| Posunout plátno | Prostřední tlačítko myši + tažení |
| Přiblížení / oddálení | Ctrl + kolečko myši |
| Nástroj Štětec | Klávesa `B` |
| Nástroj Guma | Klávesa `E` |
| Nástroj Čára | Klávesa `L` |
| Nástroj Kružnice | Klávesa `C` |
| Nástroj Obdélník | Klávesa `R` |
| Zpět (Undo) | `Ctrl + Z` |
| Znovu (Redo) | `Ctrl + Y` |
| Uložit | `Ctrl + S` |
| Uložit jako | `Ctrl + Shift + S` |
| Otevřít | `Ctrl + O` |
| Nové plátno | `Ctrl + N` |

---

## 🗂️ Struktura projektu

```
PaintApp/
├── src/
│   └── paintapp/
│       ├── Main.java            # Vstupní bod – spouští EDT
│       ├── Tool.java            # Výčet nástrojů
│       ├── CanvasPanel.java     # Plátno – veškerá logika kreslení
│       ├── ToolPanel.java       # Levý panel – nástroje, paleta, slajdry
│       ├── MainWindow.java      # Hlavní okno – menu, layout, soubory
│       ├── StatusBar.java       # Spodní lišta – poloha, zoom, undo
│       └── SettingsDialog.java  # Modální dialog nastavení
├── out/                         # Zkompilované .class soubory
├── PaintApp.jar                 # Spustitelný JAR
└── README.md
```

---


Komunikace mezi třídami probíhá tak, že `ToolPanel` i `StatusBar` dostávají
referenci na `CanvasPanel` jako konstruktorový parametr. Volají pak jeho
veřejné settery (`setCurrentTool`, `setBrushSize` …) nebo čtou stav
(`getZoomFactor`, `hasUndo` …). `CanvasPanel` si drží referenci na
`StatusBar` a po každé změně stavu zavolá `statusBar.update(this)`.

---

## 📄 Dokumentace tříd

---

### `Main.java` – vstupní bod

**Proč existuje:**  
Odděluje spouštěcí logiku od aplikační logiky. `main()` by měl dělat
co nejméně – jen nastartovat správnou věc správným způsobem.

**Jak funguje:**  
`SwingUtilities.invokeLater()` zařadí vytvoření `MainWindow` do fronty
*Event Dispatch Thread* (EDT). EDT je jediné vlákno, na kterém smí Swing
číst a měnit komponenty – bez toho hrozí závodní podmínky a vizuální
artefakty.

```
main()
  └─▶ SwingUtilities.invokeLater(lambda)
         └─▶ new MainWindow().setVisible(true)   ← spustí se na EDT
```

---

### `Tool.java` – výčet nástrojů

**Proč existuje:**  
Pojmenovává všechny nástroje jako konstanty. Bez enum bychom museli
používat čísla (`0` = štětec, `1` = guma …), což je nečitelné
a náchylné k chybám.

**Jak funguje:**  
Jednoduchý `enum` se šesti hodnotami. `CanvasPanel` drží pole
`currentTool` tohoto typu a v `switch` blocích rozhoduje, co se má
při pohybu myši nakreslit. `ToolPanel` volá `canvas.setCurrentTool(Tool.BRUSH)`
atd. po kliknutí na tlačítko.

```
Tool.BRUSH       → freehand stroke
Tool.ERASER      → paints white
Tool.LINE        → straight line (drag preview)
Tool.CIRCLE      → ellipse outline (drag preview)
Tool.RECTANGLE   → rect outline (drag preview)
Tool.FILL        → BFS flood fill on click
```

---

### `CanvasPanel.java` – jádro aplikace

**Proč existuje:**  
Odděluje veškerou logiku kreslení od zbytku UI. `MainWindow` neví nic
o tom, jak se kreslí – jen předá referenci dál.

**Jak funguje – přehled bloků:**

#### Dvojice `image` + `g2d`
`BufferedImage image` je „papír" – paměťová bitmapa, do které se ukládají
všechny tahy. `Graphics2D g2d` je „pero" – objekt, přes který se do
obrazu kreslí (`drawLine`, `fillOval` …).  
`paintComponent` pak pouze zkopíruje hotový obraz na obrazovku pomocí
`g.drawImage(image, ...)`. Díky tomu přetahování okna nebo zoom obraz
nijak nemaže.

#### `makeG2D` – zakázané vyhlazování
Anti-aliasing na obrazu se záměrně **vypíná**. Vyhlazení by mísilo
hrany tahů se sousedními pixely (černý tah na bílém plátně → šedé
přechodové pixely). Flood fill porovnává pixely pomocí `==` na celém
integeru RGB, takže by tyto smíšené pixely způsobily, že fill „vyteče"
ven nebo se zastaví předčasně. Vyhlazení je zapnuto pouze v
`paintComponent` pro vizuální zobrazení na obrazovce, kde nemůže
poškodit uložená data.

#### Undo / Redo – zásobníkový vzor
```
saveSnapshot()  →  undoStack.push(copyImage(image))
undo()          →  redoStack.push(aktuální), image = undoStack.pop()
redo()          →  undoStack.push(aktuální), image = redoStack.pop()
```
Každá operace uloží **celý snímek obrazu** před provedením. Je to
jednoduché a spolehlivé – funguje pro všechny nástroje bez výjimky.
Paměťová náročnost je omezena `maxUndoSteps`.

#### `restoreImage` – proč `AlphaComposite.SRC`
Standardní compositing `SRC_OVER` mísí zdrojový pixel přes cílový
s ohledem na alfa kanál. Pokud `g2d` stále nese composit z posledního
tahu (např. opacity 0,5), pak by `SRC_OVER` nakreslil jen 50 % uloženého
snímku přes stávající obraz – špatný výsledek.  
`AlphaComposite.SRC` **zcela nahradí** cílové pixely zdrojovými bez
ohledu na cokoli. To je správné chování pro obnovení snímku.

#### Náhled tvarů (scratch)
```
mousePressed  →  shapeScratch = copyImage(image)   ← záloha před tahem
mouseDragged  →  restoreImage(shapeScratch)         ← vymaž náhled
               →  drawShape(...)                    ← nakresli znovu
mouseReleased →  saveSnapshot()                     ← ulož do undo
               →  restoreImage(shapeScratch)
               →  drawShape(...)                    ← finální tvar
```
Bez zálohy by se při každém drag eventu přidával nový tvar na předchozí
a plátno by bylo plné překrývajících se náhledů.

#### `paintImmediately` vs `repaint`
`repaint()` je **asynchronní** – zařadí požadavek do fronty EDT.
Při rychlém pohybu myši se naakumuluje mnoho drag eventů, obraz se
aktualizuje okamžitě, ale obrazovka se překreslí až later. Tah pak
vizuálně „zaostává" za kurzorem.  
`paintImmediately()` je **synchronní** – překreslí panel okamžitě před
návratem z metody. Pro štětec a gumu je tohle nutné.

#### Ctrl + scroll zoom – střed v kurzoru
```
imgX = mouseX / staréZoom          ← pixel pod kurzorem
nový zoom = starý ± delta
scrollX = imgX * novýZoom - mouseX ← posuň lištu tak,
                                      aby imgX zůstal pod kurzorem
```

#### Flood fill – BFS
Rekurze by přetekla zásobník volání na velkém plátně. BFS frontou
navštíví každý pixel nejvýše jednou díky poli `visited[][]`.
Pixely se kódují jako `(y << 16) | x` – jeden `int` místo objektu
`Point`, méně GC tlaku.

#### `toCanvas(screenCoord)`
Jediný řádek `return (int)(screenCoord / zoomFactor)` převede
souřadnice obrazovky na souřadnice obrazu. Kdyby se zapomněl,
tah by se při zoomu kreslil na špatném místě.

---

### `ToolPanel.java` – levý panel

**Proč existuje:**  
Odděluje UI nástrojů od logiky kreslení. `ToolPanel` volá pouze
veřejné settery na `CanvasPanel` – sám nic nekreslí. Tohle je záměrný
design: každá třída má jednu zodpovědnost.

**Jak funguje – přehled bloků:**

#### Tlačítka nástrojů
`GridLayout(4, 2)` vytvoří mřížku 4×2. Každé tlačítko dostane lambda
`e -> canvas.setCurrentTool(Tool.XXX)` – po kliknutí nastaví aktivní
nástroj na plátně.

#### Paleta barev
Pole `PALETTE[]` drží 20 předpřipravených barev. Pro každou barvu se
vytvoří `JLabel` s barevným pozadím jako swatch. `MouseAdapter` rozlišuje
levé a pravé tlačítko: levé nastaví primární barvu, pravé sekundární.

#### Displej primární / sekundární barvy
Dva překrývající se `JLabel` ve fixní vrstvě (`null` layout). Kliknutí
na ně otevře `JColorChooser` pro výběr vlastní barvy. Po výběru se
zavolá `refreshColorDisplay()`, který aktualizuje pozadí obou labelu.

#### Slajdry (velikost, krytí)
`JSlider` s `ChangeListener` – na každý pohyb slajdru se okamžitě zavolá
`canvas.setBrushSize()` nebo `canvas.setOpacity()` a aktualizuje se
textový label s hodnotou.

---

### `MainWindow.java` – hlavní okno

**Proč existuje:**  
Drží pohromadě celý layout aplikace a menu. Stará se o soubory
(otevřít / uložit) a klávesové zkratky. Je to „dirigent" – koordinuje
ostatní třídy, ale sám nekreslí.

**Jak funguje – přehled bloků:**

#### Konstruktor – sestavení layoutu
```
JFrame (BorderLayout)
  WEST   → ToolPanel
  CENTER → JScrollPane → CanvasPanel
  SOUTH  → StatusBar
```
`JScrollPane` obaluje plátno, aby se dalo posouvat při přiblížení.
Hned po vytvoření scroll pane se volá `canvas.setScrollPane(sp)` –
bez toho by zoom neuměl přepočítat polohu lišt.

#### `buildMenuBar` – hierarchie menu
```
JMenuBar
  └─ JMenu "File"
       └─ JMenuItem "Save"  ← setAccelerator(Ctrl+S)
                               addActionListener(e -> saveFile())
  └─ JMenu "Edit" …
  └─ JMenu "View" …
```
Každá položka je vytvořena pomocnou metodou `item()`, která nastaví
text, klávesovou zkratku i mnemonic (podtržené písmeno pro Alt+klávesa).

#### `saveFile(forceChooser)`
Parametr `forceChooser` řídí, zda se zobrazí dialog:
- `false` = uloží do `currentFile` bez dotazu (pokud existuje)
- `true` = vždy zobrazí „Uložit jako" dialog

`ImageIO.write(image, "PNG", file)` uloží `BufferedImage` na disk.
Operace je zabalena v `try/catch IOException`, protože zápis na disk
může selhat.

#### `openFile`
`ImageIO.read(file)` načte soubor jako `BufferedImage`.
Pak se `canvas.resizeCanvas()` přizpůsobí rozměrům obrázku
a načtený obraz se nakreslí přes `g2d` do plátna.

#### `registerKeyboardShortcuts`
`KeyboardFocusManager.addKeyEventDispatcher()` zachytí klávesy globálně –
i když je focus na jiné komponentě (např. slajdru). Bez toho by zkratky
`B`, `E`, `L`… nefungovaly, pokud uživatel právě nepsal do text pole.

#### `confirmDiscardChanges`
Vzor „ochrana před ztrátou dat": před každou destruktivní akcí (nové
plátno, otevřít soubor, zavřít okno) se zkontroluje příznak `modified`.
Pokud je nastaven, zobrazí se `JOptionPane` s dotazem.

---

### `StatusBar.java` – spodní lišta

**Proč existuje:**  
Soustřeďuje zobrazování informací o stavu na jedno místo. Bez dedikované
třídy by se `JLabel` aktualizovaly roztroušeně po celém kódu.

**Jak funguje – přehled bloků:**

#### `update(CanvasPanel canvas)`
Jediná metoda, která aktualizuje **všechny** dynamické štítky najednou.
`CanvasPanel` ji volá po každé operaci, která mění stav. Tak jsou
všechna zobrazení vždy synchronizovaná bez pollingu nebo timerů.

```
update() volá se po:
  saveSnapshot()   → undo/redo stav
  undo() / redo()  → undo/redo stav
  applyZoom()      → zoom %
  resizeCanvas()   → Canvas: W×H
```

#### Undo / Redo indikátor
```java
canvas.hasUndo() ? "✔" : "✖"
```
`hasUndo()` jen zkontroluje `undoStack.isEmpty()`. Jednoduché a přímé.

#### `attachMouseTracking`
Přidá `MouseMotionAdapter` na plátno. Na každý `mouseMoved` a
`mouseDragged` přepočítá souřadnice z obrazovkových na obrazové
(`/ zoomFactor`) a aktualizuje `positionLabel`. Souřadnice jsou
oříznuty na rozsah obrazu, takže nikdy neukazují záporné hodnoty
nebo hodnoty větší než rozměry plátna.

---

## 👤 Autor

- Autor: Samuel Svoboda  
- Jazyk: **Java 11+**  
- UI framework: **Java Swing** (žádné externí závislosti)
