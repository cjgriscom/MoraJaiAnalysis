import MoraJaiWasm from './morajai_wasm.js';
import morajai_svg from './morajai_svg.js';

class MoraJaiSimulator {
    constructor(canvasId, initialSpoilerMode, onResizeCanvas, onUpdateState, onSolved) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) {
            console.error(`Canvas element with id "${canvasId}" not found.`);
            return;
        }
        this.ctx = this.canvas.getContext('2d');
        this.onResizeCanvasCallback = onResizeCanvas;
		this.onUpdateStateCallback = onUpdateState;
		this.onSolvedCallback = onSolved;

        this.spoilerMode = initialSpoilerMode;
        this.actionIcons = false;
        
        this.lastContainerWidth = "";
        this.svgElement = null;
        this.originalStyles = {};

        this.colors = {
            'GY': '#666666', 'OR': '#FF7F00', 'GN': '#009915', 'BK': '#010101',
            'RD': '#e61010', 'WH': '#EEEEEE', 'PI': '#ff73ef', 'PU': '#751f84',
            'YE': '#e8d221', 'BU': '#002af2'
        };

        this.iconsUnicode = {
            'GY': "\uEB99",
            'WH': "\uF537 \uF53B",
            'PU': "\uEA4C",
            'YE': "\uEA76",
            'GN': "\uF4D2",
            'PI': "\uF33F",
            'BK': "\uF2E5",
            'RD': "\uF1CF",
            'OR': "\uEDFC",
            'BU': "\uECD3"
        };

        this.stickerBBox = {
            'T0': '45.993,45.753,33.578,33.578', 'T1': '88.369,45.753,33.578,33.578',
            'T2': '130.654,45.753,33.578,33.578', 'T3': '45.993,88.127,33.578,33.578',
            'T4': '88.369,88.127,33.578,33.578', 'T5': '130.654,88.127,33.578,33.578',
            'T6': '45.993,130.457,33.578,33.578', 'T7': '88.369,130.457,33.578,33.578',
            'T8': '130.654,130.457,33.578,33.578', 'GY0': '18.285,18.323,21.619,21.619',
            'GY1': '170.243,18.085,21.619,21.619', 'GY2': '170.499,170.043,21.619,21.619',
            'GY3': '18.341,169.982,21.619,21.619'
        };
        
        this.targetColors   = ['GY','GY','GY','GY'];
        this.targetTiles    = ["GY","GY","GY","GY","GY","GY","GY","GY"];
        this.activeStickers = ["GY","GY","GY","GY","GY","GY","GY","GY","GY","0","0","0","0"];

        this.svgOverlayName = canvasId + "-svg-overlay";

        this.PRESS_OK = 0; this.PRESS_RESET = 1; this.PRESS_COMPLETED = 2;

    }

	setInitialState(targetColors, targetTiles, activeStickers) {
        this.wasSolved = false;
		this.targetColors = targetColors;
		this.targetTiles = targetTiles;
		this.activeStickers = activeStickers;
	}

    start() {
        this.loadSVG();
        this.resizeCanvas(false);
        window.addEventListener('resize', () => this.resizeCanvas());
        window.addEventListener('load', () => this.resizeCanvas());
        setTimeout(() => this.watchdogLoop(), 2500);
        window.addEventListener('load', () => setTimeout(() => this.watchdogLoop(), 2500));
    }

    setSpoilerMode(mode) { this.spoilerMode = mode; }
    getSpoilerMode() { return this.spoilerMode; }

    setActionIcons(icons) { this.actionIcons = icons; }
    
    watchdogLoop() {
        setTimeout(() => this.watchdogLoop(), 2500);
        if (this.canvas) {
            const imageData = this.ctx.getImageData(this.canvas.width/2, this.canvas.height/2, 1, 1);
            if ((imageData.data[0] === 0 && imageData.data[1] === 0 && imageData.data[2] === 0) || (imageData.data[3] !== 255)) {
                console.log("Reloading canvas");
                this.lastContainerWidth = "";
                this.resizeCanvas(true);
            }
        }
    }

    loadSVG(url) {
        // Base64 decode morajai_svg
        const svgText = atob(morajai_svg);
        this.svgElement = new DOMParser().parseFromString(svgText, 'image/svg+xml').documentElement;
        
        const viewBox = this.svgElement.getAttribute('viewBox');
        if (!viewBox) { console.error("SVG has no viewBox attribute."); return; }
        const [minX, minY, vbWidth, vbHeight] = viewBox.split(' ').map(parseFloat);

        this.minX = minX;
        this.minY = minY;
        this.vbWidth = vbWidth;
        this.vbHeight = vbHeight;

        this.render("Loading...");
        this.onSVGLoad();
    }

    setPathColor(pathId, color) {
        if (!this.svgElement) return;
        const path = this.svgElement.getElementById(pathId);
        if (path) {
            let style = this.originalStyles[pathId] || path.getAttribute('style') || '';
            this.originalStyles[pathId] = style;
            path.setAttribute('style', style.match(/fill:[^;]+;/) ? style.replace(/fill:[^;]+;/, `fill: ${color};`) : `${style}fill: ${color};`);
        }
    }

    setPathHidden(pathId) {
        if (!this.svgElement) return;
        const path = this.svgElement.getElementById(pathId);
        if (path) {
            if (!this.originalStyles[pathId]) this.originalStyles[pathId] = path.getAttribute('style');
            path.setAttribute('style', "opacity:0;");
        }
    }

    resizeCanvas(r = true) {
        const containerWidth = this.canvas.parentElement.clientWidth;
        const containerWidthRounded = Math.round(containerWidth * 1000);
        if (containerWidthRounded === this.lastContainerWidth) return;
        
        this.lastContainerWidth = containerWidthRounded;
        this.canvas.width = containerWidth;
        this.canvas.height = containerWidth;

        if (this.onResizeCanvasCallback) this.onResizeCanvasCallback(containerWidth);
        if (r) this.render();
    }
    
    render(text = null) {
        if (text !== null) this.render_text = text;

        Object.keys(this.stickerBBox).forEach((key, i) => this.setPathColor(key, this.colors[this.activeStickers[i]]));

        for (let i = 0; i < 4; i++) {
            const targetColor = this.targetColors[i];
            const visibleColorKey = this.getSpoilerMode() ? targetColor : "GY";
            if (this.getSpoilerMode()) {
                this.setPathHidden("GY"+i);
                this.setPathColor("C"+i, "#1a1a1a");
            } else {
                this.setPathColor("C"+i, this.colors[targetColor]);
            }
            Object.keys(this.colors).forEach(color => {
                if (color === visibleColorKey) {
                    const isActive = this.activeStickers[i+9] === "1";
                    this.setPathColor(color+i, isActive ? this.colors[this.targetColors[i]] : (targetColor === "GY" ? "#1a1a1a" : "#666666"));
                } else {
                    this.setPathHidden(color+i);
                }
            });
        }
        
        const fillText = (txt) => {

            if (this.actionIcons) {
                this.ctx.save();
                try {
                    const [minX, minY, vbWidth, vbHeight] = [this.minX, this.minY, this.vbWidth, this.vbHeight];
                    const scaleX = this.canvas.width / vbWidth, scaleY = this.canvas.height / vbHeight;
                    
                    var fontSize = this.canvas.width / 18;
                    this.ctx.font = Math.round(fontSize) + 'px remixicon';
                    this.ctx.fillStyle = 'white';
                    this.ctx.textAlign = 'center';
                    this.ctx.textBaseline = 'middle';
        
                    if (!this.spoilerMode) {
                        for (let i = 0; i < 4; i++) {
                            const bbox = this.stickerBBox["GY"+i].split(",");
                            const centerX = ((bbox[0] - minX) * scaleX + (bbox[2] - minX) * scaleX / 2);
                            const centerY = ((bbox[1] - minY) * scaleY + (bbox[3] - minY) * scaleY / 2);
                            const targetColor = this.targetColors[i];
                            const icon = this.iconsUnicode[targetColor];
                            this.ctx.save();
                            this.ctx.translate(centerX, centerY);
                            if (this.targetColors[i] === "GN") {
                                if (i === 0 || i === 2) this.ctx.rotate(Math.PI/2);
                            }
                            if (this.activeStickers[i+9] === "0") {
                                icon.split(" ").forEach(icon => this.ctx.fillText(icon, 0,0));
                            }
                            this.ctx.restore();
                        }
                    }

                    for (let i = 0; i < 9; i++) {
                        var activeColor = this.activeStickers[i];
                        this.ctx.fillStyle = getContrastColor(this.colors[activeColor]);
                        //if (activeColor === "BU") activeColor = activeStickers[4];
                        const bbox = this.stickerBBox["T"+i].split(",");
                        const centerX = ((bbox[0] - minX) * scaleX + (bbox[2] - minX) * scaleX / 2);
                        const centerY = ((bbox[1] - minY) * scaleY + (bbox[3] - minY) * scaleY / 2);
                        var icon = this.iconsUnicode[activeColor];
                        this.ctx.save();
                        this.ctx.translate(centerX, centerY);
                        if (activeColor === "GN") {
                            if (i === 0) this.ctx.rotate(Math.PI/4*2);
                            if (i === 1) this.ctx.rotate(Math.PI/4*3);
                            if (i === 2) this.ctx.rotate(0);
                            if (i === 3) this.ctx.rotate(Math.PI/4*1);
                            if (i === 4) icon = "\u2022";
                            if (i === 5) this.ctx.rotate(Math.PI/4*1);
                            if (i === 6) this.ctx.rotate(0);
                            if (i === 7) this.ctx.rotate(Math.PI/4*3);
                            if (i === 8) this.ctx.rotate(Math.PI/4*2);
                        }
                        icon.split(" ").forEach(icon => this.ctx.fillText(icon, 0,0));
                        this.ctx.restore();
                    }
                } catch (e) {
                    console.error(e);
                }
                this.ctx.restore();
            }

            if (txt) {
                this.ctx.save();
                Object.assign(this.ctx, { font: '500 40px serif', fillStyle: 'white', shadowColor: 'black', shadowBlur: 5, shadowOffsetX: 2, shadowOffsetY: 2, textAlign: 'center', textBaseline: 'middle' });
                this.ctx.fillText(txt, this.canvas.width / 2.0, this.canvas.height / 2.0);
                this.ctx.restore();
            }
        };

        if (this.svgElement) {
            const svgStr = new XMLSerializer().serializeToString(this.svgElement);
            const url = URL.createObjectURL(new Blob([svgStr], {type: 'image/svg+xml'}));
            const img = new Image();
            img.onload = () => {
                this.ctx.drawImage(img, 0, 0, this.canvas.width, this.canvas.height);
                URL.revokeObjectURL(url);
                fillText(this.render_text);
                if (!this.render_text) this.updateClickableOverlay();
            };
            img.src = url;
        } else {
            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            fillText(this.render_text);
        }
    }

    updateClickableOverlay() {
        let overlay = document.getElementById(this.svgOverlayName);
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = this.svgOverlayName;
            Object.assign(overlay.style, { position: 'absolute', zIndex: "10", pointerEvents: "none" });
            this.canvas.parentElement.appendChild(overlay);
        }
        Object.assign(overlay.style, { top: `${this.canvas.offsetTop}px`, left: `${this.canvas.offsetLeft}px`, width: `${this.canvas.clientWidth}px`, height: `${this.canvas.clientHeight}px` });
        overlay.innerHTML = "";
        
        const [minX, minY, vbWidth, vbHeight] = [this.minX, this.minY, this.vbWidth, this.vbHeight];
        const scaleX = this.canvas.clientWidth / vbWidth, scaleY = this.canvas.clientHeight / vbHeight;
        
        Object.keys(this.stickerBBox).forEach(key => {
            if (this.svgElement.getElementById(key)) {
                const bbox = this.stickerBBox[key].split(",");
                const a = document.createElement('a');
                a.href = '#';
                Object.assign(a.style, {
                    position: 'absolute', left: `${(bbox[0] - minX) * scaleX}px`, top: `${(bbox[1] - minY) * scaleY}px`,
                    width: `${bbox[2] * scaleX}px`, height: `${bbox[3] * scaleY}px`,
                    backgroundColor: 'rgba(0,0,0,0.0)', pointerEvents: 'auto'
                });
                a.addEventListener('click', (e) => { e.preventDefault(); this.svgElementClicked(key); });
                overlay.appendChild(a);
            }
        });
    }

    svgElementClicked(elementName) {
        const r = elementName.startsWith('T') ? this.press_tile(elementName.slice(-1)) : this.press_outer(elementName.slice(-1));
        this.updateState(r);
        this.render();
    }
    
    wasm_init() {
        let initStr = `${this.targetColors.join(',')}_${this.targetTiles.join(',')}_${this.activeStickers.slice(0,9).join(',')}`;
        this.morajai_module.ccall('init_from_string', 'number', ['string'], [initStr]);
    }
    
    get_state() { return this.morajai_module.UTF8ToString(this.morajai_module.ccall('get_state', 'number', [], [])); }
    press_tile(idx) { return this.morajai_module._wasm_press_tile(idx); }
    press_outer(idx) { return this.morajai_module._wasm_press_outer(idx); }

    updateState(ret = 0) {
        const [tiles, outers] = this.get_state().split('_');
        const tileColors = tiles.split(','), outerColors = outers.split(',');
        for (let i = 0; i < 9; i++) this.activeStickers[i] = tileColors[i];
        for (let i = 0; i < 4; i++) this.activeStickers[i+9] = outerColors[i];

        if (!this.wasSolved) {
		    this.onUpdateStateCallback(this.targetColors, this.targetTiles, this.activeStickers);
        }

        const isSolved = ret === this.PRESS_COMPLETED;
        this.render(isSolved ? "Solved" : "");

        if (isSolved && !this.wasSolved) this.handleSolved();

        this.wasSolved = isSolved;
    }
    
    handleSolved() {
        this.onSolvedCallback();
    }
    
    onSVGLoad() {
        MoraJaiWasm().then(Module => {
            this.morajai_module = Module;
            this.wasm_init();
            this.updateState();
            this.render("");
        }).catch(err => { console.error('Error initializing WASM module:', err); this.render("Error"); });
    }

    onSVGError() { this.render("Error"); }

    loadPuzzle(stickers, targets) {
        this.wasSolved = false;
        this.targetColors = targets;
        this.targetTiles = stickers.slice(0, 9);
        this.activeStickers.splice(0, stickers.length, ...stickers);
        
        if (this.morajai_module) {
            this.wasm_init();
            this.updateState();
        }
    }
}

function getContrastColor(bgColor) {
    // https://stackoverflow.com/questions/3942878/how-to-decide-font-color-in-white-or-black-depending-on-background-color/3943023#3943023
    // Convert hex to RGB
    const hex = bgColor.replace('#', '');
    const r = parseInt(hex.substr(0, 2), 16);
    const g = parseInt(hex.substr(2, 2), 16);
    const b = parseInt(hex.substr(4, 2), 16);
    
    // Calculate relative luminance
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    
    // Return black or white based on luminance
    return luminance > 0.5 ? '#010101' : '#FFFFFF';
}

window['MoraJaiSimulator'] = MoraJaiSimulator;
export default MoraJaiSimulator;