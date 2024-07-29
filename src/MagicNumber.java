<template>
  <div id="p5root" ref="p5root">
    <RulerVue
      v-if="store.scaleChecked"
      :rulerOffsetX="rulerOffsetX"
      :rulerOffsetY="rulerOffsetY"
      :rulerWidth="rulerWidth"
      :rulerHeight="rulerHeight"
      :meterForPixel="meterForPixel"
      :zoom="zoom"
      :rulerCall="rulerCall"
    ></RulerVue>
    <div
      class="center"
      id="p5Container"
      :style="styleObject"
      ref="p5Container"
    ></div>
    <ErrorPopup
      :message="$t('map.file.not.found')"
      v-if="store.mapLoadError"
      @submit="onSubmit()"
    ></ErrorPopup>
  </div>
</template>

<script>
import ErrorPopup from "./ErrorPopup.vue";
import { uploadPoiFile } from "../apiUtility/apiutil";
import { getUrl, devmodeImageUrl, getEllipsisText } from "../utility/util";
import p5 from "p5";
import axios from "axios";
import mergeImages from "merge-images";
import { decode } from "base64-arraybuffer";
import FloodFill from "q-floodfill";
import RulerVue from "./RulerVue.vue";
import getAuthToken from "../util";
const SCROLL_BAR_SIZE = 5;
const HANDLE_RADIUS = 15;
import { usePopUpStore } from "../stores/store";
import dialougeMixin from "../mixins/dialouge.mixins";
export default {
  name: "P5Map",
  components: { RulerVue, ErrorPopup },
  mixins: [dialougeMixin],
  emits: [
    "setcurrentMapImage",
    "setcurrentMapChanged",
    "currentMap",
    "clearNodeData",
    "selectedObjectChanged",
    "loadingDone",
    "mapImage",
    "undoredoChanged",
    "backToMenuSelect",
    "clearPoiData",
    "zoomIn",
    "zoomOut",
    "p5MouseMoved",
    "UndoAndRedoImageBuffer",
    "currentObj",
    "selectedObjXEnter",
    "selectedObjYEnter",
    "zoomValue",
  ],
  props: [
    "poiPosition",
    "selectedNodeProperty",
    "currentDomain",
    "showDialogAutoCreate",
    "subMenuSelection",
    "pencilSize",
    "drawColor",
    "colorPlateModel",
    "currentMap",
    "showGrid",
    "drawer",
    "map",
    "mapInfoZPosition",
    "hilightEdge",
    "autoCreateType",
    "autoCreateLayout",
    "autoCreateCount",
    "autoCreateInterval",
    "autoCreatBaseNode",
    "menuSelection",
    "defaultAttr",
  ],
  data() {
    return {
      tempGuideline: null,
      currentSubMenuSelection: this.subMenuSelection,
      maxOrderValues: {},
      zoomFromMouse: false,
      sf: 1,
      //Diameter of POI
      DIAMETER: 0.6,
      // Edge settings
      edgeGroups: new Map(),
      edgeMap: new Map(),
      nodeMap: new Map(),
      //edgeSet: new Set(),
      edgeSelected: false,
      nodeDelete: false,
      isDeleteEdgeGroup: false,
      initialCreateEdgeGroup: true,
      selectedEdges: new Set(),
      selectedEdgesFormatted: new Set(),
      edgesGrouped: false,
      currentSelectedEdge: null,
      currentSelectedEdgeGroup: null,
      selectedEdgesFromGroup: [],
      blindAlleysArray: [],
      ///currentBlindAlley: 0,
      edgeArrowSize: 0.4,
      // POI Elevator
      tempArrPoi: [],
      mouseMovePosition: null,
      elevTextWidth: null,
      rotateArray: [],
      angle: 0,
      arrSelectedShapes: [],
      isSelectingObject: false,
      elevatorPoi: [],
      draggingObj: [],
      arrPoi: [],
      remainArrPoi: [],
      popup: false,
      tooltipName: "",
      recordPOIPos: true,
      initialPOIPos: new Map(),
      finalPOIPos: new Map(),
      changePOIPos: [],
      getZero: "",
      takeShapeSnapshot: null,
      combinedShapeData: null,
      combinedPixelData: null,
      nodePOIValidated: null,
      imageContextForValidation: null,
      shapeContextForValidation: null,
      imageContextSub: null,
      //
      selectedProperty: null,
      stopSaving: false,
      savingShape: false,
      mergeImg: false,
      saveBool: false,
      //
      shapeContext: undefined,
      uploadURL: null,
      authToken: null,
      reqPath: null,
      cb: null,
      drawnLines: [],
      //Undo Redo
      undoStack: [],
      redoStack: [],
      originalImageValue: {},
      countSize: 0,
      countLevel: 0,
      //drag
      dragflag: false,
      isPainting: false,
      isDragging: false,
      previousStoredImages: [],
      pasteIndex: 0,
      stopSelection: false,
      //copy paste
      initialSelection: {},
      showSelection: false,
      isSelecting: false,
      imageContext: {},
      htmlContext: {},
      selection: { x: 0, y: 0, w: 0, h: 0 },
      copiedImageData: {},
      pastePosition: {},
      isPasting: false,
      isPasted: false,
      imageSourceData: {},
      isDraggable: false,
      currentX: 0,
      currentY: 0,
      PastedArrayPositions: [],
      isCtrlPressed: false,
      imageNode: null,
      imageNodeElevator: null,
      nodeNameHeader: null,
      lastNewNode: null,
      arrAutoNode: [],
      mouseDraggedPosition: null,
      dragStartPosition: { x: 0, y: 0 },
      mouseMoveOffset: null,
      colorGuideLine: null,
      guideLine: null,
      arrSelectedObjects: [],
      arrHandles: [],
      selectedHandle: null,
      undoBuffer: [],
      redoBuffer: [],
      draggingObject: [],
      meterForPixel: 0.1,
      drawingShape: null,
      paintPos: { x: 0, y: 0 },
      isLineEnd: false,
      arrLine: [],
      arrAllLines: [],
      isLineDrawing: false,
      linePoints: {}, // start x & y coordinates
      scaleChecked: true,
      p5: null,

      scrollBar: {
        horizontal: {
          offset: 0,
          size: 14,
          name: "horizontal",
        },
        vertical: {
          offset: 0,
          size: 14,
          name: "vertical",
        },
      },

      viewPort: {
        x: 0,
        y: 0,
        width: 0,
        height: 0,
      },

      zoom: 1.0,
      selectedScrollbar: null,
      mapImage: null,
      loadImageWidth: 0,
      loadImageHeight: 0,
      currentmap: this.currentMap,
      rulerOffsetX: 0,
      rulerOffsetY: 0,
      rulerWidth: 0,
      rulerHeight: 0,
      rulerCall: null,
      mousePressPosition: null,
      store: usePopUpStore(),
      //Draggable selection
      clickedInsideSelection: false,
      topLeftHandle: false,
      topRightHandle: false,
      bottomLeftHandle: false,
      bottomRightHandle: false,
      pencilLineDrawing: false,
      dragTopLeftPos: {},
      styleObject: {
        left: "30px",
        top: "29px",
        right: "0px",
        bottom: "0px",
        display: "flex",
        position: "absolute",
      },
      polygonBool: null,
      addingValue: 0.7,
      arrAutoPoi: [],
      firstFlag: false,
      mx: null,
      my: null,
    };
  },
  computed: {
    // isEdgeUpdated() {
    //   return this.store.updateSelectedEdgeGroup
    // },
    currentBlindAlley() {
      return this.store.edge_property.length - 1;
    },
    isEdgeSettingModeEnabled() {
      return (
        this.subMenuSelection === "edgeSetting" || this.store.edgeHighlightFlag //subMenuSelection is edgeSetting means edgeHighlightFlag is true
      );
    },
    rotatePoiZoom() {
      let origin = this.mapCoordinateFromScreenCoordinate({
        x: this.store.elevatorPosition.x,
        y: this.store.elevatorPosition.y,
      });

      let newXPos =
        origin.x -
        (1.5 / this.zoom) * Math.cos((90 - this.angle) * (Math.PI / 180));
      let newYPos =
        origin.y +
        (1.5 / this.zoom) * Math.sin((90 - this.angle) * (Math.PI / 180));

      let newXPosBtn =
        origin.x -
        (3.4 / this.zoom) * Math.cos((90 - this.angle) * (Math.PI / 180));
      let newYPosBtn =
        origin.y +
        (3.4 / this.zoom) * Math.sin((90 - this.angle) * (Math.PI / 180));

      let newElevPos = this.screenCoordinateFromMapCoordinate({
        x: newXPos,
        y: newYPos,
      });

      let newElevPosBtn = this.screenCoordinateFromMapCoordinate({
        x: newXPosBtn,
        y: newYPosBtn,
      });
      return {
        newElevPos: newElevPos,
        newElevPosBtn: newElevPosBtn,
      };
    },
    imageDownloaded() {
      return this.store.getLoadImage;
    },
    arrShape() {
      if (this.currentMap != null && this.currentMap.data != null)
        return this.currentMap.data.Shape;
      return [];
    },
    arrNode() {
      if (this.currentMap != null && this.currentMap.data != null) {
        return this.currentMap.data.Node;
      }
      return [];
    },
    // arrPoi() {
    //   if (this.currentMap != null && this.currentMap.data != null)
    //     return this.currentMap.poi.customPointData;
    //   return [];
    // },
  },
  watch: {
    arrSelectedObjects: {
      handler(val) {
        if (val.length === 0) {
          this.store.createPoiLineUp = false;
        }
      },
    },
    zoom: {
      handler(newVal, oldVal) {
        if (newVal !== oldVal) {
          this.$emit("zoomValue", newVal);
        }
      },
    },
    currentSubMenuSelection: {
      handler(newVal, oldVal) {
        console.log("newVal,oldVal", newVal, oldVal);
        if (
          newVal == null &&
          oldVal === "ctrlHandDrag" &&
          localStorage.getItem("pencilFlag") === "true"
        )
          this.$emit("subMenuChanged", "menuPencil");
      },
    },
    "store.selectedProperty": {
      handler(val) {
        if (val === "POI") this.store.drawerSelection = val.toLowerCase();
        if (val === this.$t("common.node"))
          this.store.drawerSelection = this.$t("common.node").toLowerCase();
      },
    },
    "store.updateSelectedEdgeGroup": {
      handler(val) {
        if (!val && this.store.currentSelectedEdgeGroupID !== "None") {
          this.highlightEdgeGroup(this.store.currentSelectedEdgeGroupID);
        }
      },
    },
    arrNode: {
      handler(val) {
        this.createNodeMap(val);
        this.createEdgeMap(val);
        if (this.initialCreateEdgeGroup) this.createEdgeGroupMap(val); // !this.isDeleteEdgeGroup ||
        if (this.store.currentSelectedEdgeGroupID !== "None")
          this.highlightEdgeGroup(this.store.currentSelectedEdgeGroupID);
      },
      deep: true,
      immediate: true,
    },
    // arrNode: {
    //   handler(val) {
    //     this.createEdgeGroupMap(val);
    //     //this.currentBlindAlley++;
    //   },
    // },
    isEdgeSettingModeEnabled: {
      handler(val) {
        this.currentSelectedEdgeGroup = null;
        this.selectedEdges.clear();
        this.store.edgeExistsInGroup = false;
        this.store.edgeBelongsToGroup = false;

        if (val === false) {
          //console.warn("EDGE SETTING MODE DISABLED");
          this.currentSelectedEdge = null;
          //this.selectedEdges.clear();
        } else {
          //console.warn("EDGE SETTING MODE ENABLED");
        }
      },
    },
    selectedEdges: {
      handler(val) {
        const formattedSet = [...val].map((item) => {
          if (typeof item === "string") return JSON.parse(item);
          else if (typeof item === "object") return item;
        });
        this.selectedEdgesFormatted = formattedSet;
        if (
          this.selectedEdgesFormatted.length === 0 ||
          this.selectedEdgesFormatted.length === undefined
        ) {
          this.store.disableGroupAddBtn = true;
          this.store.disableEdgeAddBtn = true;
          this.store.disableEdgeDelBtn = true;
        } else {
          this.store.disableGroupAddBtn = false;
          if (this.store.currentSelectedEdgeGroupID !== "None") {
            this.store.disableEdgeAddBtn = false;
            this.store.disableEdgeDelBtn = false;
          }
        }
        // if (this.store.currentSelectedEdgeGroupID === "None") {
        //   // this.store.disableEdgeAddBtn = true;
        //   // this.store.disableEdgeDelBtn = true;
        // } else if(this.selectedEdgesFormatted.length === 0 ||
        //   this.selectedEdgesFormatted.length === undefined) {
        //   this.store.disableEdgeAddBtn = true;
        //   this.store.disableEdgeDelBtn = true;
        // } else {
        //   this.store.disableEdgeAddBtn = false;
        //   this.store.disableEdgeDelBtn = false;
        // }
      },
      deep: true,
      immediate: true,
    },
    "store.currentSelectedEdgeGroupID": {
      handler(val) {
        if (val === "None") {
          this.store.disableEdgeAddBtn = true;
          this.store.disableEdgeDelBtn = true;
          this.store.disableGroupDelBtn = true;
          this.currentSelectedEdgeGroup = null;
        } else {
          this.store.disableGroupDelBtn = false;
          if (this.selectedEdgesFormatted.length !== 0) {
            this.store.disableEdgeAddBtn = false;
            this.store.disableEdgeDelBtn = false;
          }
          this.highlightEdgeGroup(this.store.currentSelectedEdgeGroupID);
        }
      },
    },
    currentSelectedEdgeGroup: {
      handler(val) {
        if (val?.length === 0 || val === null || val === undefined) {
          this.store.disableGroupDelBtn = true;
          this.store.currentSelectedEdgeGroupID = "None";
          this.store.disableEdgeAddBtn = true;
          this.store.disableEdgeDelBtn = true;
        } else {
          this.store.disableGroupDelBtn = false;
          if (
            this.selectedEdgesFormatted.length !== 0 ||
            this.selectedEdgesFormatted.length === undefined
          ) {
            //this.store.disableEdgeAddBtn = false;
            //this.store.disableEdgeDelBtn = false;
          }
        }
      },
      deep: true,
      immediate: true,
    },
    menuSelection: {
      handler(val) {
        //cleared unfinished polygon
        if (val) {
          this.drawingShape = null;
          this.guideLine = null;
          this.resetElevatorPoi();
          this.selection = {};
        }
      },
    },
    currentmap: {
      handler(val) {
        this.$emit("currentMap", val);
        if (val && val.data) {
          this.arrPoi = this.removeDuplicates(
            this.currentMap.poi.customPointData,
            "cpId"
          );
        }
        const tempMeterForPixel = val?.data?.MapInfo?.MeterForPixel;
        if (tempMeterForPixel !== null) {
          this.meterForPixel = tempMeterForPixel;
        }
        if (val?.data?.MapInfo?.Features) {
          this.store.showPOILineUp = val.data.MapInfo.Features[0]?.LineupPOI;
        }
      },
      deep: true,
      immediate: true,
    },

    imageDownloaded: function (newVal) {
      if (newVal) {
        this.$nextTick(() => {
          setTimeout(() => {
            this.loadMapImage(() => {
              this.$emit("loadingDone", this);
              //reset center margin
              this.styleObject.left = "30px";
              this.styleObject.top = "29px";
              this.styleObject.right = "0px";
              this.styleObject.bottom = "0px";
            });
          }, 1500);
        });
      }
    },
    subMenuSelection: function (newVal, oldVal) {
      console.log("newVal,oldVal submenu", newVal, oldVal);
      this.currentSubMenuSelection = newVal;
      this.store.autoCreateBool = false;
      this.store.optionFlag = newVal;
      if (oldVal === "newPoi") {
        this.store.currentChildgid = "";
        this.store.currentChildOrder = 0;
      }
      this.resetElevatorPoi();
      if (["alignNode", "copyPoi"].includes(newVal)) return;
      if (newVal === "menuPaint") this.p5.cursor(this.p5.ARROW);
      if (newVal === "newPoi" || newVal === "AutoCreatePOI") {
        // this.takeShapeSnapshot = true;
        this.getCtxData();
      }
      if (newVal == "menuSelect") {
        this.p5.cursor(this.p5.ARROW);
        ////rain: here check. how to destroy
        if (this.lastNewNode != null) this.lastNewNode = null;
        if (this.lastNewPoi != null) this.lastNewPoi = null;
        if (this.drawingShape != null && this.drawingShape.shape_type == "line")
          this.finishLineDraw();
        this.guideLine = null;
      } else if (["menuPencil"].includes(newVal)) {
        this.p5.cursor(this.p5.ARROW);
        this.isLineDrawing = false;
        this.arrLine = [];
      } else if (
        newVal == "newNode" ||
        newVal == "newPoi" ||
        newVal === "AutoCreatePOI" ||
        newVal === "menuElevator"
      ) {
        this.p5.cursor(this.p5.ARROW);
        if (!this.store.createPoiLineUp) this.selectObject(null, false);
      } else if (newVal == "menuHandDrag" || newVal == "ctrlHandDrag") {
        this.p5.cursor(this.p5.HAND);
      } else if (newVal == "menuPencil") {
        //this.p5.cursor(this.p5.CROSS);
        if (
          !this.pointInsideRect(
            this.p5.mouseX,
            this.p5.mouseY,
            this.scrollBar.horizontal.offset + 42,
            this.p5.height - SCROLL_BAR_SIZE - 55,
            this.scrollBar.horizontal.size - 85,
            SCROLL_BAR_SIZE
          ) &&
          !this.pointInsideRect(
            this.p5.mouseX,
            this.p5.height - this.p5.mouseY,
            this.p5.width - SCROLL_BAR_SIZE - 5,
            this.scrollBar.vertical.offset + SCROLL_BAR_SIZE,
            SCROLL_BAR_SIZE,
            this.scrollBar.vertical.size
          )
        )
          this.p5.noCursor();
        else this.p5.cursor();
      } else {
        this.p5.cursor(this.p5.ARROW);
        this.selectObject(null, false);
      }
    },
  },

  methods: {
    async deleteEdgeFromGroupsPostNodeDelete(node) {
      let linkedNodes = this.findNodesIncludeEdge(node.ID);
      console.log("Linked nodes and node are ->> ", linkedNodes, node);
      for await (const linkedNode of linkedNodes) {
        //--------------LOOP START--------------------------
        let edge = `${node.ID}_${linkedNode.ID}`;
        let revEdge = `${linkedNode.ID}_${node.ID}`;
        let edgeToDelete = null;

        // Check which one is valid edge name
        // by checking in edge map

        if (this.edgeMap.has(edge)) {
          edgeToDelete = edge;
        } else if (this.edgeMap.has(revEdge)) {
          edgeToDelete = revEdge;
        } else {
          edgeToDelete = null;
        }

        let ba = null;

        //----------------------------
        if (edgeToDelete !== null) {
          // find BlindAlley and directly fetch from EdgeGroupMap
          // OR
          // here first check which group does the edge
          // belongs to

          for await (const [key, value] of this.edgeGroups) {
            for await (const val of value) {
              if (val.edge === edgeToDelete) {
                ba = key;
                break;
              }
            }
            if (ba !== null) break;
          }
        }
        // else {
        //   return;
        // }

        if (ba !== null && edgeToDelete !== null) {
          let edgeGroup = this.edgeGroups.get(ba);
          let arr = JSON.parse(JSON.stringify(edgeGroup));
          for await (const [index, val] of arr.entries()) {
            if (val.edge === edgeToDelete) {
              arr.splice(index, 1);
            }
          }
          if (arr.length > 0) {
            this.edgeGroups.set(ba, arr);
            await this.deleteEdgeProperty(edgeToDelete);
          } else if (arr.length === 0) {
            await this.deleteEdgeGroup(ba);
          }
        }

        //--------------END OF OUTER LOOP--------------------------
      }
    },
    isClickNearSlantEdge(edgeInfo, screenCoord) {
      let xPosA = edgeInfo.pos[0].x;
      let xPosB = edgeInfo.pos[1].x;
      let yPosA = edgeInfo.pos[0].y;
      let yPosB = edgeInfo.pos[1].y;
      let xPosC = screenCoord.x;
      let yPosC = screenCoord.y;

      let distancePointAndLineNum = Math.abs(
        (xPosB - xPosA) * (yPosC - yPosA) - (yPosB - yPosA) * (xPosC - xPosA)
      );
      let distancePointAndLineDen = Math.sqrt(
        Math.pow(xPosB - xPosA, 2) + Math.pow(yPosB - yPosA, 2)
      );
      let distancePointAndLine =
        distancePointAndLineNum / distancePointAndLineDen;

      let bool = distancePointAndLine < 10 ? false : true;
      return bool;
    },
    dragSelectEdges() {
      if (this.guideLine === null) return;
      if (!this.isEdgeSettingModeEnabled) return;
      for (const [edge, edgeInfo] of this.edgeMap) {
        if (
          this.pointInsideX1Y1X2Y2(
            edgeInfo.pos[0].x,
            edgeInfo.pos[0].y,
            this.guideLine.x,
            this.guideLine.y,
            this.guideLine.x2,
            this.guideLine.y2
          ) &&
          this.pointInsideX1Y1X2Y2(
            edgeInfo.pos[1].x,
            edgeInfo.pos[1].y,
            this.guideLine.x,
            this.guideLine.y,
            this.guideLine.x2,
            this.guideLine.y2
          )
        ) {
          this.selectedEdges.add(
            JSON.stringify({
              edge: edge,
              edgeInfo: edgeInfo,
            })
          );
          this.currentSelectedEdge = edge;
          this.edgeSelected = true;
          // check if the edge is in some group and select the group
          if (this.edgeSelected) this.selectEdgeGroup(this.currentSelectedEdge);
        }
      }
      if (this.isEdgeSettingModeEnabled) this.arrSelectedObjects = [];
    },
    centerMapFocusing() {
      //map focusing to center
      this.viewPort.y = (this.loadImageHeight - this.p5.height) / 2;
      this.viewPort.y += SCROLL_BAR_SIZE + 5;

      this.viewPort.x = (this.loadImageWidth - this.p5.width) / 2;
      this.viewPort.x += SCROLL_BAR_SIZE + 5;
    },
    boundaryCheckForPoi(screenCoordinate) {
      //boundary check for moving poi
      let radius = this.DIAMETER / this.meterForPixel / 2;
      for (let theta = 0; theta <= 360; theta += 10) {
        let arcCordinate = this.circleXY(radius, theta);

        this.getPixelValAtPos(
          Math.floor(screenCoordinate.x + arcCordinate.x),
          this.mapImage.height -
            Math.floor(screenCoordinate.y + arcCordinate.y),
          this.imageContextForValidation
        );
        this.getShapeDataAtPos(
          Math.floor(screenCoordinate.x + arcCordinate.x),
          this.mapImage.height -
            Math.floor(screenCoordinate.y + arcCordinate.y),
          this.shapeContextForValidation
        );

        if (
          this.combinedPixelData === 0 ||
          this.combinedPixelData === 450 ||
          this.combinedShapeData === "00255"
        ) {
          return true;
        }
      }
    },
    getValueWithZoomMultiplier(val) {
      return val / this.zoom;
    },
    getValueWithMeterForPixelMultiplier(val) {
      return val / this.meterForPixel;
    },
    //Utility method to calculate coordinate of arc of circle on theta angle
    circleXY(r, theta) {
      // Convert angle to radians
      theta = ((theta - 90) * Math.PI) / 180;

      return { x: r * Math.cos(theta), y: -r * Math.sin(theta) };
    },
    disableEdgeSettingBtns() {
      /*
      - disable Group ADD When there is no selected edge
      - Group (DELETE): When no group is selected (‘None’ status in the group list)
      - Edge (ADD, DELETE):
      
      -When no group is selected ('None' status in group list)
        Edge is a function to add or delete additional edges to an already existing group. If no groups are selected, there is no need to enable them.
      
      -When no edge is selected
      (If there are no edges highlighted in yellow on the canvas)
      This is because if no edges are selected,
      you cannot add or delete edges.
    
      -After adding a group, there are red highlighted edges 
        but there are no selected edges (yellow highlighted edges), 
        so the Add/Delete Edge buttons should be disabled.
    
      
      */
    },
    addNewEdgeGroup() {
      if (this.store.edgeBelongsToGroup) {
        this.store.edgeExistsInGroup = true;
        this.store.edgeBelongsToGroup = false;
        this.selectedEdges.clear();
        this.selectedEdgesFromGroup = [];
        return;
      }
      // else {
      //   this.store.edgeExistsInGroup = false;
      // }

      if (this.selectedEdgesFormatted.length === 0) return;
      ///this.currentBlindAlley++;
      // console.warn("The selected edges to be added in group are-->>");
      if (this.currentBlindAlley !== undefined)
        this.store.edge_property.push(`BlindAlley: ${this.currentBlindAlley}`);

      this.edgesGrouped = true;
      // this.assignEdgeProp = `BlindAlley${i++}`;

      //Storing the created group in edgeGroups
      this.edgeGroups.set(
        `BlindAlley: ${this.currentBlindAlley}`,
        this.selectedEdgesFormatted
      );
      this.store.currentSelectedEdgeGroupID = `BlindAlley: ${this.currentBlindAlley}`;

      // change properties in arrNode
      for (const edge of this.selectedEdgesFormatted) {
        let edgeIDs = edge.edge.split("_");

        let firstNode = edgeIDs[0];
        let secondNode = edgeIDs[1];

        // swap the edges and addProperty function

        this.addEdgeProperty(firstNode, secondNode);

        // for swaped edges

        firstNode = edgeIDs[1];
        secondNode = edgeIDs[0];

        this.addEdgeProperty(firstNode, secondNode);
      }
      // raincheck -> below code commented?
      this.currentSelectedEdgeGroup = this.selectedEdgesFormatted;
      this.store.hideCurrentEdgeGroup = false;

      // once edges are added in group, lose reference to selectedEdges.
      this.selectedEdges.clear();
    },
    addEdgeInGroup() {
      let edgeToAdd = null;
      this.selectedEdgesFormatted.every((edge) => {
        edgeToAdd = edge;
        for (const [key, edgeGroup] of this.edgeGroups) {
          for (const groupedEdge of edgeGroup) {
            if (edge.edge === groupedEdge.edge) {
              this.store.edgeExistsInGroup = true;
              break;
            }
          }
          if (this.store.edgeExistsInGroup) break;
        }
        if (this.store.edgeExistsInGroup) return false;
        else return true;
      });
      if (!this.store.edgeExistsInGroup) {
        this.selectedEdgesFormatted.forEach((edge) =>
          this.edgeGroups.get(this.store.currentSelectedEdgeGroupID).push(edge)
        );

        // change properties in arrNode
        for (const edge of this.selectedEdgesFormatted) {
          let edgeIDs = edge.edge.split("_");

          let firstNode = edgeIDs[0];
          let secondNode = edgeIDs[1];

          // swap the edges and addProperty function

          this.addEdgeProperty(firstNode, secondNode);

          // for swaped edges

          firstNode = edgeIDs[1];
          secondNode = edgeIDs[0];

          this.addEdgeProperty(firstNode, secondNode);
        }
      }
      // raincheck below
      //this.store.edgeExistsInGroup = false;
      this.selectedEdges.clear();
    },
    async deleteEdgeProperty(edge) {
      /* should return in case of node
        deletion
        fn call in rearrangeGroup method
      */
      let firstNode = edge.split("_")[0];
      let secondNode = edge.split("_")[1];
      await this.deleteNodeEdgeProp(firstNode, secondNode);
      await this.deleteNodeEdgeProp(secondNode, firstNode);
    },
    async deleteNodeEdgeProp(node1, node2) {
      for await (const node of this.arrNode) {
        if (node.ID === node1) {
          // check if Edge array is not present in node
          if (
            node.Edge === undefined ||
            node.Edge === null ||
            node.Edge?.length === 0
          )
            continue;

          for await (const edge of node.Edge) {
            if (
              edge.E_Node_Number === node2 &&
              edge.hasOwnProperty("Property")
            ) {
              delete edge.Property;
            }
          }
        }
      }
    },

    async deleteEdgeGroup(groupID) {
      this.isDeleteEdgeGroup = true;
      this.store.currentSelectedEdgeGroupID = "None";
      this.currentSelectedEdgeGroup = [];
      // if (!this.nodeDelete)
      this.store.edge_property.pop();
      //this.store.edge_property.splice(this.store.edge_property.indexOf(groupID), 1);

      // Delete the group (individual edge property) from arrNode
      if (this.edgeGroups.has(groupID)) {
        for await (const edge of this.edgeGroups.get(groupID)) {
          this.deleteEdgeProperty(edge.edge);
        }
      }

      // this.edgeGroups.delete(groupID);
      let deletedBlindAlley = groupID.split(" ")[1];

      //------------testing for delete node------
      let edgeGroups = new Map(this.edgeGroups);

      for (const [key, value] of edgeGroups) {
        if (parseInt(key.split(" ")[1]) > parseInt(deletedBlindAlley)) {
          this.edgeGroups.set(
            `BlindAlley: ${parseInt(key.split(" ")[1]) - 1}`,
            value
          );
          this.edgeGroups.delete(key);
        } else if (
          parseInt(key.split(" ")[1]) === parseInt(deletedBlindAlley)
        ) {
          this.edgeGroups.delete(key);
        }
      }
      //----------------------------------------------

      this.selectedEdges.clear();
      this.store.edgeBelongsToGroup = false;
      // Shift all the consecutive BlindAlley by -1

      // if(this.nodeDelete) {
      //   for(let i = 0; i < this.blindAlleysArray.length; i++) {
      //     this.shiftEdgePropAfterDelete(parseInt(this.blindAlleysArray[i]) - i);
      //   }
      // } else {
      await this.shiftEdgePropAfterDelete(deletedBlindAlley);
      // }
      // this.createEdgeGroupMap(this.arrNode);
      // this.isDeleteEdgeGroup = false;
    },
    async shiftEdgePropAfterDelete(BlindAlley) {
      // only target nodes from edgeGroup Map -> raincheck
      for await (const node of this.arrNode) {
        // check if Edge array is not present in node
        if (
          node.Edge === undefined ||
          node.Edge === null ||
          node.Edge?.length === 0
        )
          continue;

        for await (const edge of node.Edge) {
          if (
            edge.hasOwnProperty("Property") &&
            Object.keys(edge.Property).length > 0 &&
            parseInt(edge.Property.BlindAlley) > parseInt(BlindAlley)
          ) {
            edge.Property.BlindAlley = (
              parseInt(edge.Property.BlindAlley) - 1
            ).toString();
          }
        }
      }
    },
    deleteEdgeInGroup() {
      let arr = this.edgeGroups.get(this.store.currentSelectedEdgeGroupID);
      let selectedEdgeArr = JSON.parse(
        JSON.stringify(this.selectedEdgesFormatted)
      );

      // Check whether there is any edge that is not part of group-----
      for (const selectedEdge of this.selectedEdgesFormatted) {
        let index = null;
        // Iterate through selectedEdges, compare each selectedEdge
        for (const edge of arr) {
          if (edge.edge === selectedEdge.edge) {
            index = selectedEdgeArr.indexOf(selectedEdge);
            selectedEdgeArr.splice(index, 1);
            break;
          }
        }
      }

      if (selectedEdgeArr.length !== 0) {
        this.store.deleteEdgePopup = true;
        this.selectedEdges.clear();
        return;
      } else {
        // Operations to delete the edges in group
        for (const selectedEdge of this.selectedEdgesFormatted) {
          for (const edge of arr) {
            if (edge.edge === selectedEdge.edge) {
              let index = arr.indexOf(edge);
              arr.splice(index, 1);
              this.deleteEdgeProperty(edge.edge);
              break;
            }
          }
        }

        // add condition if arr.length === 0, then delete the GroupID from edgeGroups
        if (arr.length === 0) {
          // this.edgeGroups.delete(this.store.currentSelectedEdgeGroupID);
          // this.store.currentSelectedEdgeGroupID = "None";.
          this.deleteEdgeGroup(this.store.currentSelectedEdgeGroupID);
        } else {
          this.edgeGroups.set(this.store.currentSelectedEdgeGroupID, arr);
          this.highlightEdgeGroup(this.store.currentSelectedEdgeGroupID);
        }
        this.store.edgeBelongsToGroup = false;
      }

      this.currentSelectedEdge = null;
      this.selectedEdges.clear();
    },
    addEdgeProperty(firstNode, secondNode) {
      // Add property in arrNode
      for (const node of this.arrNode) {
        if (node.ID == firstNode) {
          // here for secondNode, add the property

          for (const edgeNode of node.Edge) {
            if (edgeNode.E_Node_Number === secondNode) {
              edgeNode.Property = {
                BlindAlley: this.store.currentSelectedEdgeGroupID
                  .split(" ")[1]
                  .toString(),
              };
            }
          }
        }
      }
    },
    highlightEdgeGroup(val) {
      if (val === "None") {
        this.currentSelectedEdgeGroup = [];
        return;
      }
      console.log("Inside highlightEdgeGroup fn------>", val);
      this.store.currentSelectedEdgeGroupID = `BlindAlley: ${
        val.split(" ")[1]
      }`;
      this.currentSelectedEdgeGroup = this.edgeGroups.get(
        this.store.currentSelectedEdgeGroupID
      );
      this.store.hideCurrentEdgeGroup = false;
    },
    drawHighlightEdge(p5, edge, group) {
      /*

      This function is used to highlight a single edge. It will be called
      on selectedEdgesFormatted set in draw() function.
      */
      if (
        this.currentSelectedEdgeGroup === null &&
        this.selectedEdgesFormatted.size === 0
      )
        return;
      p5.push();
      p5.strokeWeight(5);

      /*
      If edges belong to a group, then color them red,
      else color them yellow -->raincheck
      */
      if (group) {
        p5.stroke(255, 0, 0, 255 * 0.7);
      } else {
        p5.stroke(255, 215, 0, 255 * 0.7);
      }

      p5.line(
        edge.edgeInfo.pos[0].x,
        edge.edgeInfo.pos[0].y,
        edge.edgeInfo.pos[1].x,
        edge.edgeInfo.pos[1].y
      );

      p5.pop();
    },
    selectEdge(screenCoord) {
      /*

      This function will select a single edge and then call selectEdgeGroup function
      to select the group which that edge belongs to.
      */

      if (this.getObjectAtMouse(screenCoord.x, screenCoord.y) == "node") {
        this.selectedEdges.clear();
        return;
      }
      this.edgeSelected = false;

      //----------------function starts----------------------

      for (const [edge, edgeInfo] of this.edgeMap) {
        let skipEdge = true;
        let boundingBox = this.calcBoundingBoxForEdge(
          edgeInfo.pos,
          Math.round(edgeInfo.equation[0])
        );
        let boundingBoxWidth = null;
        let boundingBoxHeight = null;
        if (
          Math.round(edgeInfo.equation[0]) !== 0 &&
          Math.round(edgeInfo.equation[0]) !== Infinity && // Slanted Edges cases
          Math.round(edgeInfo.equation[0]) !== -0 &&
          Math.round(edgeInfo.equation[0]) !== -Infinity
        ) {
          boundingBoxWidth = boundingBox.width;
          boundingBoxHeight = boundingBox.height;
          // Additional distance check FOR SLANTED EDGES(Distance btw line and point)
          // Line passing through point A and point B
          // MouseClick Position is point C

          skipEdge = this.isClickNearSlantEdge(edgeInfo, screenCoord);

          //-------------------------------------------------------------------------
        } else {
          boundingBoxWidth = boundingBox.width / this.meterForPixel;
          boundingBoxHeight = boundingBox.height / this.meterForPixel;
          skipEdge = false;
        }
        let coordinate = this.getRectangleCoordinate(edgeInfo.pos);
        if (this.isPointInsideRectangle(screenCoord, coordinate) && !skipEdge) {
          if (
            this.selectedEdges.has(
              JSON.stringify({
                edge: edge,
                edgeInfo: edgeInfo,
              })
            )
          ) {
            this.selectedEdges.delete(
              JSON.stringify({
                edge: edge,
                edgeInfo: edgeInfo,
              })
            );
            if (this.selectedEdgesFromGroup.includes(edge)) {
              this.selectedEdgesFromGroup.splice(
                this.selectedEdgesFromGroup.indexOf(edge),
                1
              );
              if (this.selectedEdgesFromGroup.length === 0)
                this.store.edgeBelongsToGroup = false;
            }
            //this.edgeSelected = false;
          } else {
            this.selectedEdges.add(
              JSON.stringify({
                edge: edge,
                edgeInfo: edgeInfo,
              })
            );
            this.currentSelectedEdge = edge;
            this.edgeSelected = true;
            // check if the edge is in some group and select the group
            if (this.edgeSelected)
              this.selectEdgeGroup(this.currentSelectedEdge);
          }
        }
        if (this.edgeSelected) break;
      }
      // if (!this.edgeSelected) {
      //   this.selectedEdges.clear();
      // }
    },
    dist(p1, p2) {
      const { x: x1, y: y1 } = p1;
      const { x: x2, y: y2 } = p2;

      const distance = Math.sqrt((x2 - x1) ** 2 + (y2 - y1) ** 2);

      return distance;
    },
    rotate(point, center, angle) {
      const x =
        center.x +
        (point.x - center.x) * Math.cos(angle) -
        (point.y - center.y) * Math.sin(angle);
      const y =
        center.y +
        (point.x - center.x) * Math.sin(angle) +
        (point.y - center.y) * Math.cos(angle);
      return { x, y };
    },
    getRectangleCoordinate(rectangle) {
      //const {x:p1, y:p2} = point;
      //const {x:x1, y:y1} = rectangle[0];
      //const {x:x2, y:y2} = rectangle[1];
      const p11 = rectangle[0];
      const p22 = rectangle[1];
      const angle = Math.atan2(p22.y - p11.y, p22.x - p11.x);
      const c = {
        x: p11.x + (p22.x - p11.x) / 2,
        y: p11.y + (p22.y - p11.y) / 2,
      };
      const w = this.dist(p11, p22);
      const h = 10;
      let p1 = {
        x: c.x - w / 2,
        y: c.y - h / 2,
      };
      let p2 = {
        x: c.x + w / 2,
        y: c.y - h / 2,
      };
      let p3 = {
        x: c.x + w / 2,
        y: c.y + h / 2,
      };
      let p4 = {
        x: c.x - w / 2,
        y: c.y + h / 2,
      };
      return [
        this.rotate(p1, c, angle),
        this.rotate(p2, c, angle),
        this.rotate(p3, c, angle),
        this.rotate(p4, c, angle),
      ];
    },
    selectEdgeGroup(edge) {
      /* This function will check if the selected edge belongs to
          some group
      */
      //console.warn("SELECT EDGE GROUP FN CALLED", edge);
      let firstNode = edge.split("_")[0];
      let secondNode = edge.split("_")[1];

      for (const node of this.arrNode) {
        if (node.ID === firstNode) {
          node.Edge.forEach((edgeNode) => {
            if (edgeNode.E_Node_Number === secondNode) {
              //check whether second node E_Node_Number has 'Property' key
              if (
                edgeNode.hasOwnProperty("Property") &&
                Object.keys(edgeNode.Property).length > 0
              ) {
                /* Below line will select the edgeGroup 
                  which the selected edge belongs to 
                */
                // this.currentSelectedEdgeGroup = this.edgeGroups.get(
                //   `BlindAlley: ${edgeNode.Property.BlindAlley}`
                // );

                // To only select but not highlight the currentSelectedGroup - changed
                // if (
                //   this.store.currentSelectedEdgeGroupID !==
                //   `BlindAlley: ${edgeNode.Property.BlindAlley}`
                // ) {
                //   this.store.hideCurrentEdgeGroup = true;

                // }
                this.selectedEdgesFromGroup.push(edge);
                this.store.edgeBelongsToGroup = true;

                //this.store.currentSelectedEdgeGroupID = `BlindAlley: ${edgeNode.Property.BlindAlley}`;

                // this.selectedEdgesFormatted = [];
              } else {
                //this.store.hideCurrentEdgeGroup = false;
                //-- Group Add operation fix-----
                //this.currentSelectedEdgeGroup = [];

                // raincheck below comment- done
                //this.store.edgeBelongsToGroup = false;
                this.highlightEdgeGroup(this.store.currentSelectedEdgeGroupID);
                //-------
              }
            }
          });
        }
      }
    },
    createEdgeMap(arrNode) {
      this.edgeMap.clear();
      if (arrNode.length === 0) return;
      function swapEdges(edge) {
        let [e1, e2] = edge.split("_");
        return `${e2}_${e1}`;
      }
      // this only works for already saved nodes and edges, for additional nodes/edges, write condition here
      //----------------handled by watcher----------------
      for (const node of arrNode) {
        if (
          node.Edge === undefined ||
          node.Edge === null ||
          node.Edge?.length === 0
        )
          continue;
        for (const edge of node.Edge) {
          let edgeName = `${node.ID}_${edge.E_Node_Number}`;

          // check for a non existing node in Edge info
          if (!this.nodeMap.has(edge.E_Node_Number)) continue;

          // here condition-> edgeMap should not already have the edge after swap function
          let reversedEdge = swapEdges(edgeName);
          if (this.edgeMap.has(reversedEdge)) continue;

          let firstNodeScreenPos = this.screenCoordinateFromMapCoordinate(
            node.Position
          );

          let secondNodeScreenPos = this.screenCoordinateFromMapCoordinate(
            this.nodeMap.get(edge.E_Node_Number)
          );
          let edgePosition = [firstNodeScreenPos, secondNodeScreenPos];

          // here m and c will be according to map coordinates, not screen coordinates
          const { m, c } = this.lineEquationFromPoints(
            { x: firstNodeScreenPos.x, y: firstNodeScreenPos.y },
            { x: secondNodeScreenPos.x, y: secondNodeScreenPos.y }
          );
          let lineCoefficients = [m, c];

          let edgeInfo = {
            pos: edgePosition,
            equation: lineCoefficients,
          };

          this.edgeMap.set(edgeName, edgeInfo);

          // if (m === 0 || m === Infinity) {
          //   // horizontal edge or vertical edge
          //   // here calc the bounding box/edge end points and add as value in edgemap

          //   this.edgeMap(edgeName, { edgePosition: edgePosition });
          // } else {
          //   //slanted edge
          //   //here add slope and constant as value in edgemap

          //   this.edgeMap.set(edgeName, { lineCoefficients: lineCoefficients,  });
          // }

          //let boundingBox = this.calcBoundingBoxForEdge(edgePosition);
          //-----------------------------------------
        }
      }
    },
    createNodeMap(arrNode) {
      this.nodeMap.clear();
      if (arrNode.length === 0) return;
      for (const node of arrNode) {
        this.nodeMap.set(node.ID, node.Position);
      }
    },
    createEdgeGroupMap(arrNode) {
      this.edgeGroups.clear();
      this.store.edge_property = ["None"];
      if (arrNode.length === 0) return;
      for (let node of arrNode) {
        if (
          node.Edge === undefined ||
          node.Edge === null ||
          node.Edge?.length === 0
        )
          continue;
        for (let edgeNode of node.Edge) {
          if (
            edgeNode.hasOwnProperty("Property") &&
            Object.keys(edgeNode.Property).length > 0 &&
            this.nodeMap.has(edgeNode.E_Node_Number)
          ) {
            // && checking for node existence
            if (
              !this.edgeGroups.has(
                `BlindAlley: ${edgeNode.Property.BlindAlley}`
              )
            ) {
              this.store.edge_property.push(
                `BlindAlley: ${edgeNode.Property.BlindAlley}`
              );

              this.edgeGroups.set(
                `BlindAlley: ${edgeNode.Property.BlindAlley}`,
                []
              );
            }
            /// if (this.currentBlindAlley < edgeNode.Property.BlindAlley && !this.isDeleteEdgeGroup) {
            ///   this.currentBlindAlley = edgeNode.Property.BlindAlley;
            /// }
            // Adding proxy edge values in "edgeGroupsMap"
            let edge = `${node.ID}_${edgeNode.E_Node_Number}`;

            if (this.edgeMap.has(edge)) {
              //console.warn("Edge in Edge group->>", edge);
              // let edgeNotFound = true;
              // for(const [key, value] of this.edgeGroups) {
              //   for(const groupedEdge of value) {
              //     if(groupedEdge.edge === edge) {
              //       edgeNotFound = false;
              //     }
              //   }
              // }
              this.edgeGroups
                .get(`BlindAlley: ${edgeNode.Property.BlindAlley}`)
                .push({
                  edge: edge,
                  edgeInfo: this.edgeMap.get(edge),
                });
            }

            // check for swapped edges. eg: Edge[N1_N2] -> Edge[N2_N1]
          }
        }
      }
      this.isDeleteEdgeGroup = this.isDeleteEdgeGroup ? false : true;
      // else {
      //         this.isDeleteEdgeGroup = false;
      //       }
      //this.currentBlindAlley++;
      //this.initialCreateEdgeGroup = false;
    },
    calcBoundingBoxForEdge(edge, slope) {
      let minX, minY, maxX, maxY;
      let x, y;
      let width = null;
      let height = null;
      for (let pos of edge) {
        if (minX == null) minX = pos.x;
        if (minY == null) minY = pos.y;
        if (maxX == null) maxX = pos.x;
        if (maxY == null) maxY = pos.y;
        if (pos.x < minX) minX = pos.x;
        if (pos.x > maxX) maxX = pos.x;
        if (pos.y < minY) minY = pos.y;
        if (pos.y > maxY) maxY = pos.y;
      }

      // assign the first position as starting point and second position
      // as end point
      // let xPos = edge[0].x;
      // let yPos = edge[0].y;

      // distance between the two nodes will be the length/width of bounding box

      let firstNodeMapPos = this.mapCoordinateFromScreenCoordinate({
        x: edge[0].x,
        y: edge[0].y,
      });
      let secondNodeMapPos = this.mapCoordinateFromScreenCoordinate({
        x: edge[1].x,
        y: edge[1].y,
      });
      let dist = Math.sqrt(
        Math.pow(secondNodeMapPos.x - firstNodeMapPos.x, 2) +
          Math.pow(secondNodeMapPos.y - firstNodeMapPos.y, 2)
      );

      if (slope === Infinity || slope === -Infinity) {
        height = dist;
        width = 0.2;
        x = minX - 1;
        y = minY; //+ ((maxY - minY) /2);
      } else if (slope === 0 || slope === -0) {
        width = dist;
        height = 0.2;
        x = minX; //+ ((maxX - minX) /2);
        y = minY - 1;
      } else {
        x = minX;
        y = minY;
        width = maxX - minX + 1;
        height = maxY - minY + 1;
      }
      // calculate the angle counter-clockwise

      //let angle = Math.atan2(edge[1].y - edge[0].y, edge[1].x - edge[0].x);

      // return {
      //   x: xPos, //+ ((edge[1].x - edge[0].x) /2),
      //   y: yPos, //+ ((edge[1].y - edge[0].y) /2),
      //   width: dist,
      //   height: 0.25,
      //   angle: (angle * 180) / Math.PI,
      // };

      return {
        x: x,
        y: y,
        width: width, //maxX === minX ? maxX - minX + 0.25 : maxX - minX,
        height: height, //maxY === minY ? maxY - minY + 0.25 : maxY - minY,
      };
    },
    removeDuplicates(arr, property) {
      const uniqueObjects = [];
      const seen = new Map();

      for (let i = arr.length - 1; i >= 0; i--) {
        const obj = arr[i];
        const key = obj[property];

        if (!seen.has(key)) {
          seen.set(key, true);
          uniqueObjects.unshift(obj); // Add to the beginning to preserve the last occurrence
        }
      }

      return uniqueObjects;
    },
    createPOIMap(arrPOI) {
      for (const poi of arrPOI) {
        this.poiMap.set(poi.cpId, poi);
      }
    },
    async attachHandles() {
      let object = this.arrSelectedObjects[0];
      if (object.shape_type == "line" || object.shape_type == "polygon") {
        for (let point of object.Position) {
          this.arrHandles.push({
            target: object,
            targetPoint: point,
            x: point.x,
            y: point.y,
          });
        }
      } else if (
        object.shape_type == "rectangle" ||
        object.shape_type == "ellipse"
      ) {
        this.arrHandles.push({
          target: object,
          x: object.Position.x,
          y: object.Position.y,
          type: "bottomLeft",
        });
        this.arrHandles.push({
          target: object,
          x: object.Position.x + object.Size.width - 1,
          y: object.Position.y,
          type: "bottomRight",
        });
        this.arrHandles.push({
          target: object,
          x: object.Position.x + object.Size.width - 1,
          y: object.Position.y + object.Size.height - 1,
          type: "topRight",
        });
        this.arrHandles.push({
          target: object,
          x: object.Position.x,
          y: object.Position.y + object.Size.height - 1,
          type: "topLeft",
        });
      }
    },
    validatePOIRightDrawer(value, item) {
      this.getCtxData();
      // console.warn("inside function validatePOIRightDrawer", value);
      if (
        value.x !== undefined &&
        value.x !== this.arrSelectedObjects[0].Position.x &&
        ["ArrowRight", "ArrowLeft", "move"].includes(item)
      ) {
        let mapCoordinate = {
          x: value.x,
          y: this.arrSelectedObjects[0].Position.y,
        };
        let screenCoordinate =
          this.screenCoordinateFromMapCoordinate(mapCoordinate);

        this.getPixelValAtPos(
          Math.floor(screenCoordinate.x),
          this.mapImage.height - Math.floor(screenCoordinate.y),
          this.imageContextForValidation
        );
        this.getShapeDataAtPos(
          Math.floor(screenCoordinate.x),
          this.mapImage.height - Math.floor(screenCoordinate.y),
          this.shapeContextForValidation
        );
        if (
          this.combinedPixelData === 0 ||
          this.combinedPixelData === 450 ||
          this.combinedShapeData == "00255"
        ) {
          // console.warn("NOT MOVING POI");
          this.store.rightDrawerX = this.arrSelectedObjects[0].Position.x;
        } else {
          let result = this.boundaryCheckForPoi(screenCoordinate);
          if (result) return;
          if (item === "ArrowRight") {
            this.arrSelectedObjects[0].Position.x += this.store.poiMove;
          } else if (item === "ArrowLeft") {
            this.arrSelectedObjects[0].Position.x -= this.store.poiMove;
          } else {
            this.arrSelectedObjects[0].Position.x = value.x;
          }
          this.store.rightDrawerX = null;
        }
      } else {
        let mapCoordinate = {
          x: this.arrSelectedObjects[0].Position.x,
          y: value.y,
        };
        let screenCoordinate =
          this.screenCoordinateFromMapCoordinate(mapCoordinate);
        this.getPixelValAtPos(
          Math.floor(screenCoordinate.x),
          this.mapImage.height - Math.floor(screenCoordinate.y),
          this.imageContextForValidation
        );
        this.getShapeDataAtPos(
          Math.floor(screenCoordinate.x),
          this.mapImage.height - Math.floor(screenCoordinate.y),
          this.shapeContextForValidation
        );
        if (
          this.combinedPixelData === 0 ||
          this.combinedPixelData === 450 ||
          this.combinedShapeData == "00255"
        ) {
          // console.warn("NOT MOVING POI");
          this.store.rightDrawerY = this.arrSelectedObjects[0].Position.y;
        } else {
          let result = this.boundaryCheckForPoi(screenCoordinate);
          if (result) return;
          if (item === "ArrowUp") {
            this.arrSelectedObjects[0].Position.y += this.store.poiMove;
          } else if (item === "ArrowDown") {
            this.arrSelectedObjects[0].Position.y -= this.store.poiMove;
          } else {
            this.arrSelectedObjects[0].Position.y = value.y;
          }
          this.store.rightDrawerY = null;
        }
      }
    },
    getCtxData() {
      let _ctx = this.p5.createGraphics(
        this.mapImage.width,
        this.mapImage.height
      );
      _ctx.pixelDensity(1);
      _ctx.rectMode(_ctx.CENTER);
      _ctx.imageMode(_ctx.CENTER);

      _ctx.scale(1, -1);
      _ctx.translate(0, -parseInt(_ctx.height));

      if (this.store.shapeChecked) {
        for (let shape of this.arrShape) {
          this.drawShape(_ctx, shape, false);
        }
      }

      this.shapeContextForValidation = _ctx;
    },
    ctrlClickFun() {
      if (
        this.store.dragScroll.ctrl &&
        (this.store.dragScroll.drag || this.store.dragScroll.press)
      )
        return "exit";
    },
    onSubmit() {
      window.close("", "_parent", "");
    },
    autoCreatePoi() {
      this.store.autoCreateBool = true;
    },
    replacePoi(val) {
      //Replace Poi name
      let findValue = val.findPoi.split(",");
      let replaceValue = val.replacePoi.split(",");
      for (let index = 0; index < findValue.length; index++) {
        const searchElement = findValue[index];
        const replaceElement = replaceValue[index];
        if ([undefined].includes(replaceElement)) continue;
        this.arrPoi.forEach((item) => {
          if (
            item.name.en === searchElement ||
            item.name.kr === searchElement
          ) {
            item.name.en =
              item.name["en-US"] =
              item.name.kr =
              item.name["ko-KR"] =
                replaceElement;
            item.cpId = "robot-generating_".concat(
              Math.random().toString(36).substring(2, 16)
            );
          }
        });
      }
      this.$emit("backToMenuSelect");
    },
    //--------------------MAP Validation Checks---------------------------
    rightDrawerPOIValidation(val) {},
    getPixelValAtPos(xPos, yPos, ctx) {
      // console.warn("X and Y position are : ", xPos, yPos);
      let pixelData = ctx.canvas
        .getContext("2d", { willReadFrequently: true })

        .getImageData(xPos, yPos, 1, 1).data;
      this.combinedPixelData = pixelData[0] + pixelData[1] + pixelData[2];
      // console.warn("PIXEL DATA IS", pixelData[0], pixelData[1], pixelData[2]);
    },
    getShapeDataAtPos(xPos, yPos, ctx) {
      // console.warn("X and Y position wrt Shape context are : ", xPos, yPos);
      let shapeData = ctx.canvas
        .getContext("2d", { willReadFrequently: true })
        .getImageData(xPos, yPos, 1, 1).data;
      this.combinedShapeData = `${shapeData[0]}${shapeData[1]}${shapeData[2]}`; // should be "00255" for Blue shapes
      // console.warn("SHAPE CONTEXT DATA IS ", shapeData, this.combinedShapeData);
    },
    getShapeDataAtPosForPolygon(xPos, yPos, ctx) {
      let pixelColor = ctx.get(xPos, yPos);
      let color = `rgba(${pixelColor[0]},${pixelColor[1]},${pixelColor[2]},${pixelColor[3]})`;
      const rgba = color.replace(/^rgba?\(|\s+|\)$/g, "").split(",");
      const hex = `#${(
        (1 << 24) +
        (parseInt(rgba[0]) << 16) +
        (parseInt(rgba[1]) << 8) +
        parseInt(rgba[2])
      )
        .toString(16)
        .slice(1)}`;
      let finalResult = this.store.designColorUsed.includes(hex) ? true : false;
      return finalResult;
    },
    //--------------------MAP Validation Checks---------------------------
    rearrangeIndex(val) {
      let item =
        val === "node" ? this.currentmap.data.Node : this.currentmap.data.Shape;

      item.forEach((el, index) => {
        el["__index"] = index;
      });
    },
    setMeterForPixel(val) {
      this.currentmap.data.MapInfo.MeterForPixel = val;
      // Ruler의 Prop에 변동사항을 전달하기 위해서 필요
      this.meterForPixel = val;
    },
    // getImageURL(ctx){
    //   const imageData = ctx.getImageData(
    //     0,
    //     0,
    //     this.imageContext.canvas.width,
    //     this.imageContext.canvas.height
    //   );
    //   let canvas = document.createElement("canvas");
    //   let newCtx = canvas.getContext("2d");
    //   canvas.width = 2000;
    //   canvas.height = 2000;
    //   newCtx.putImageData(imageData, 0, 0);
    //   return canvas.toDataURL()
    // },

    //---------------------------
    saveOutputImage2(ctx) {
      const imageDataURL = ctx.canvas.toDataURL("image/png");
      const headers = {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": process.env.QA_URL,
        "Access-Control-Allow-Methods": "*",
        "Access-Control-Allow-Credentials": "true",
      };
      axios
        .post(
          `${getUrl()}maptestmerge`,
          { imageData: imageDataURL, sessionId: this.store.sessionId },
          {
            params: { mapFileName: this.store.mapFileName },
            headers: headers,
          }
        )
        .then((response) => {
          console.log("Image sent successfully", response);
          console.log("saveout cb");
          //this.store.toggleSavingShape(true);
          // this.savingShape = true;
          // console.log("test");
          // if (this.cb !== null) this.cb();
        })
        .catch((error) => {
          console.error("Error sending image:", error);
        });
      this.savingShape = true;
      console.log("test");
      if (this.cb !== null) this.cb();
    },
    saveShapeImage2(ctx) {
      const imageDataURL = ctx.canvas.toDataURL("image/png");
      const headers = {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": process.env.QA_URL,
        "Access-Control-Allow-Methods": "*",
        "Access-Control-Allow-Credentials": "true",
      };
      axios
        .post(
          `${getUrl()}mapteststatic`,
          { imageData: imageDataURL, sessionId: this.store.sessionId },
          {
            params: { mapFileName: this.store.mapFileName },
            headers: headers,
          }
        )
        .then((response) => {
          console.log("Image sent successfully", response);
          this.mergeImg = true;
          console.log("test");
          //this.store.toggleSavingShape(false);
          this.savingShape = false;
          console.log("test");
        })
        .catch((error) => {
          console.error("Error sending image:", error);
        });
      //this.store.toggleMergeImg(true);
    },
    saveCombinedImage2(b64) {
      //const imageDataURL = ctx.canvas.toDataURL('image/png');
      const headers = {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": process.env.QA_URL,
        "Access-Control-Allow-Methods": "*",
        "Access-Control-Allow-Credentials": "true",
      };
      axios
        .post(
          `${getUrl()}maptestslam`,
          { imageData: b64, sessionId: this.store.sessionId },
          {
            params: { mapFileName: this.store.mapFileName },
            headers: headers,
          }
        )
        .then((response) => {
          console.log("Image sent successfully", response);
        })
        .catch((error) => {
          console.error("Error sending image:", error);
        });
    },
    //---------------------------
    // Upload zipped map images to presigned URL
    uploadMapImages(url) {
      // File Upload Update -> make a post request here

      let urlObj = {
        test: "test",
        url: url,
        sessionId: this.store.sessionId,
      };
      axios
        .post(`${getUrl()}mapfileupload`, urlObj, {
          params: { mapFileName: this.store.mapFileName },
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": process.env.QA_URL,
            "Access-Control-Allow-Methods": "*",
            "Access-Control-Allow-Credentials": "true",
          },
        })
        .then((res) => {
          console.log("response after upload-->>", res);
        })
        .catch((err) => {
          console.log("error in upload", err);
        });

      /*
      axios
        .put(url, file, {
          headers: {
            "Content-Type": "application/zip",
          },
        })
        .then((res) => {
          console.log("response after upload-->>", res);
        })
        .catch((err) => {
          console.log("error in upload", err);
        });
        */
    },
    // To Get Auth-Token
    // async getAuthToken() {
    //   let randomString = btoa(Math.random().toString()).substring(10, 25);

    //   //these headers will be same for all requests
    //   const headers = {
    //     //Fixed values start
    //     "Content-Type": "application/json",
    //     "x-api-key": "89409dbbb620461ea9cbe27f903a978a",
    //     "x-country-code": "KR",
    //     "x-language-code": "ko-KR",
    //     "x-biz-channel": "CHN000081",
    //     // Fixed values end
    //     "x-user-no": "000081KR2303290361606", //for "lgsi_webmapeditor@yopmail.com"
    //     "x-user-role": "USR081001",
    //     "x-message-id": randomString,
    //   };

    //   // for getting auth-token
    //   const body = {
    //     password:
    //       "13dfda2d52a52da6958309f9f000f3ed21f862b7be8d9c475c2036d8507a837e49e16338eb442af7b674f692d1d0c712a852ebc07308437a0ebe3cc18140d5fd",
    //     otp: "",
    //     password_encrypted: "Y",
    //   };
    //   try {
    //     let res = await axios.post(
    //       "${location.protocol}//kic-qa-svc.lgerobot.com/robot/b2b/v1.1/user/lgsi_webmapeditor@yopmail.com/session",
    //       body,
    //       {
    //         headers: headers,
    //       }
    //     );
    //     this.authToken = res.data.result.authToken;
    //     return this.authToken;
    //   } catch (err) {
    //     console.log("error getting auth-token->>", err);
    //   }

    //   /*
    //     .then((res) => {
    //       console.log("response for auth-token", res);
    //       console.log("auth-token", res.data.result.authToken);
    //       // Save this auth-token in store for later use
    //       this.authToken = res.data.result.authToken;
    //     })
    //     .catch((err) => {
    //       console.log("error while getting auth-token", err);
    //     });
    //     */
    // },

    // To get Upload URL
    async getUploadURL(authToken) {
      let randomString = btoa(Math.random().toString()).substring(10, 25);
      const headers = {
        //Fixed values start
        "Content-Type": "application/json",
        "x-api-key": this.store.appKey,
        "x-country-code": "KR",
        "x-language-code": "ko-KR",
        "x-biz-channel": "CHN000081",
        // Fixed values end
        "x-user-no": this.store.userNo, //for "lgsi_webmapeditor@yopmail.com"
        "x-user-role": this.store.userRole,
        "x-message-id": randomString,
        "x-auth-token": authToken,
      };
      const body = {
        buildingIndex: this.store.buildingIndex,
        floorIndex: this.store.floorIndex,
        mapIndex: this.store.mapIndex,
        requestMap: [
          {
            mapType: "navi",
            fileName: `${this.store.mapFileName}.zip`,
          },
        ],
        generationInfo: this.store.generationInfo,
        uploader: this.store.requester,
        reason: this.store.saveMapItem.id,
      };

      try {
        let res = await axios.post(
          `${this.store.serviceHost}/robot/b2b/v1.5/resource/type/MAP_LGIDM/url`,
          body,
          {
            headers: headers,
          }
        );
        console.log("Response for upload URL->", res.data.result.naviUrl);
        return res.data.result.naviUrl;
      } catch (err) {
        console.log("error while fetching upload URL ->>", err);
      }

      /*
        .then((res) => {
          console.log("Response for upload URL->", res.data.result.naviUrl);
          return res.data.result.naviUrl;
        })
        .catch((err) => {
          console.log("error while fetching upload URL ->>", err);
        });
        */
    },
    // Save Functions
    saveMapImage(cb) {
      // main cb
      this.cb = cb;
      // this.store.setSavingBool(true);
      //     console.log("test");
      //     this.store.toggleSavingShape(false);
      //     console.log("test");
      this.saveCtxToImage(
        this.mapImage.canvas.getContext("2d", { willReadFrequently: true }),
        () => {
          //this.store.setSavingBool(true);
          this.saveBool = true;
          console.log("test");
          //this.store.toggleSavingShape(false);
          this.savingShape = false;
          this.mergeImg = false;
          console.log("test");
        }
      );
    },
    saveOutputImage(ctx) {
      this.saveCtxToImage(ctx, () => {
        console.log("saveout cb");
        //this.store.toggleSavingShape(true);
        this.savingShape = true;
        console.log("test");
        if (this.cb !== null) this.cb();
      });
    },
    saveCombinedImage(ctx) {
      this.saveCtxToImage(ctx, () => {
        console.log("saving combined images cb");
      });
    },
    saveShapeImage(ctx) {
      this.saveCtxToImage(ctx, () => {
        if (this.cb !== null) this.cb();
      });
      //this.store.toggleMergeImg(true);
      this.mergeImg = true;
      console.log("test");
      //this.store.toggleSavingShape(false);
      this.savingShape = false;
      console.log("test");
    },
    async saveCtxToImage(ctx, cb) {
      // let buffer = ctx.canvas
      //   .getContext("2d", { willReadFrequently: true })
      //   .canvas.toDataURL(0, 0, 2000, 2000)
      //   .split(";base64,")[1];
      usePopUpStore().saveFailed = false;
      let buffer = ctx.canvas.toDataURL("image/png");
      const headers = {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": process.env.QA_URL,
        "Access-Control-Allow-Methods": "*",
        "Access-Control-Allow-Credentials": "true",
      };

      if (buffer !== null) {
        // `${location.protocol}//${location.hostname}:${process.env.SERVER_PORT}/map`

        // fetch(decode(buffer)).then((res) => {
        //   console.log("Stream in browser ->>", res);
        // });

        // if(this.reqPath = "merge") {
        //   fetch(`http://127.0.0.1:9000/map`, {
        //   method: "POST",
        //   headers: headers,
        //   mode: 'no-cors',
        //   body: decode(buffer),
        // })
        //   .then((res) => {
        //     console.log("response from server ->>>", res);
        //   })
        //   .catch((err) => console.log("error while posting buffer --->>", err));
        // } else {
        axios
          .post(
            `${getUrl()}map`,
            { imageData: buffer, sessionId: this.store.sessionId },
            {
              params: {
                mapFileName: encodeURIComponent(this.store.mapFileName),
              },
              headers: headers,
              withCredentials: true,
            }
          )
          .then((res) => {
            console.log("response from server ->>>", res);
            if (cb !== null) cb();
          })
          .catch((err) => {
            usePopUpStore().saveFailed = true;
            console.log("error while posting buffer --->>", err);
          });
        //}

        // if (cb !== null) cb();
      } else {
        console.log("buffer null");
      }
    },
    //Copy Paste
    copy(ctx) {
      let x = this.selection.x;
      let y = ctx.canvas.height - this.selection.y;
      let w = this.selection.w < 0 ? this.selection.w * -1 : this.selection.w;
      let h = this.selection.h < 0 ? this.selection.h * -1 : this.selection.h;
      var imageData = ctx.getImageData(x, y, w, h);
      this.copiedImageData = imageData;
    },
    //Copy Paste
    paste(ctx) {
      var MyImage = new Image();
      MyImage.src = getImageURL(
        this.copiedImageData,
        this.selection.w,
        this.selection.h
      );
      function getImageURL(imgData, width, height) {
        let canvas = document.createElement("canvas");
        let newCtx = canvas.getContext("2d");
        canvas.width = width;
        canvas.height = height;
        if (imgData instanceof ImageData) {
          newCtx.putImageData(imgData, 0, 0);
        }
        return canvas.toDataURL(); //image URL
      }
      this.imageSourceData = MyImage;
      var ContextDetails = this;
      MyImage.onload = function () {
        if (!ContextDetails.isPasted) {
          ContextDetails._Go(ContextDetails.imageContext);
          ContextDetails.isPasted = true;
        }
      };
    },
    drawGrid(p5) {
      if (this.showGrid == false) return;
      if (this.saveBool) return;
      p5.push();
      p5.scale(1 / this.zoom, 1 / this.zoom);
      p5.strokeWeight(1);
      p5.stroke(255, 255, 255, 120);
      this.prepareDash(p5);

      let pixelForMeter = 1 / this.currentmap.data.MapInfo.MeterForPixel;
      let interval =
        this.currentMap.data.MapInfo.GridUnit * pixelForMeter * this.zoom;
      for (let x = 0; x < this.mapImage.width * this.zoom; x += interval) {
        p5.line(x, 0, x, this.mapImage.height * this.zoom);
      }

      for (let y = 0; y < this.mapImage.height * this.zoom; y += interval) {
        p5.line(0, y, this.mapImage.width * this.zoom, y);
      }
      this.finishDash(p5);
      p5.pop();
    },
    getCenter(x1, y1, x2, y2) {
      return {
        x: (x1 + x2) / 2,
        y: (y1 + y2) / 2,
      };
    },
    getRandomInt(min, max) {
      console.log("here call radam ID number");
      return Math.floor(Math.random() * (max - min)) + min;
    },
    recalcRuler() {
      let RulerSize = 30;
      this.rulerOffsetX = -this.viewPort.x + RulerSize;
      this.rulerOffsetY = this.viewPort.y + RulerSize;
      this.rulerWidth = this.viewPort.width;
      this.rulerHeight = this.p5.height - SCROLL_BAR_SIZE;
    },

    undo() {
      console.log("undo", this.undoBuffer.length);
      let item = this.undoBuffer.pop();
      if (item == null) return;
      if (item.action == "addObjects") {
        this.deleteObjects(item.objects, false);
      } else if (item.action == "deleteObjects") {
        this.addObjects(item.objects, false);
      } else if (item.action == "createEdgeLink") {
        this.deleteEdgeLink(item.fromObject, item.toObject, false);
      } else if (item.action == "deleteEdgeLink") {
        this.createEdgeLink(item.fromObject, item.toObject);
      } else if (item.action == "move" && item.objects.length) {
        item.objects.forEach((object) => {
          if (Array.isArray(object.Position)) {
            //if selected multi
            object.Position.forEach((position) => {
              position.x -= item.x_move;
              position.y -= item.y_move;
            });
            if (object.BoundingBox) {
              object.BoundingBox.x -= item.x_move;
              object.BoundingBox.y -= item.y_move;
            }
          } else {
            object.Position.x -= item.x_move;
            object.Position.y -= item.y_move;
          }
        });

        console.log("undo item", item);
      } else if (item.action == "align") {
        item.objects.forEach((item) => {
          item.node.Position.x = item.prev_x;
          item.node.Position.y = item.prev_y;
        });

        console.log("undo item", item);
      }

      if (item.action === "move") {
        if (item.objects.length) this.redoBuffer.push(item);
      } else {
        this.redoBuffer.push(item);
      }

      console.log("after undo, buff", this.redoBuffer);
      this.$emit("undoredoChanged", this.undoBuffer, this.redoBuffer);
    },
    redo(newVal, oldVal) {
      let item = this.redoBuffer.pop();
      if (item == null) return;
      console.log("redo action", item.action);
      console.log("redo item: ", item);
      if (item.action == "createEdgeLink") {
        this.createEdgeLink(item.fromObject, item.toObject);
      } else if (item.action == "deleteEdgeLink") {
        this.deleteEdgeLink(item.fromObject, item.toObject);
      } else if (item.action == "addObjects") {
        this.addObjects(item.objects, true);
      } else if (item.action == "deleteObjects") {
        this.deleteObjects(item.objects, true);
      } else if (item.action == "move" && item.objects.length) {
        item.objects.forEach((object) => {
          //////////rain
          if (Array.isArray(object.Position)) {
            object.Position.forEach((position) => {
              position.x += item.x_move;
              position.y += item.y_move;
            });
            if (object.BoundingBox) {
              object.BoundingBox.x += item.x_move;
              object.BoundingBox.y += item.y_move;
            }
          } else {
            object.Position.x += item.x_move;
            object.Position.y += item.y_move;
          }
        });
        this.addUndo(item);
      } else if (item.action == "align") {
        item.objects.forEach((item) => {
          item.node.Position.x = item.next_x;
          item.node.Position.y = item.next_y;
        });
        this.addUndo(item);
      }
      this.$emit("undoredoChanged", this.undoBuffer, this.redoBuffer);
    },

    addUndo(action) {
      if (action.action === "move") {
        if (action.objects.length) this.undoBuffer.push(action);
      } else {
        this.undoBuffer.push(action);
      }

      this.$emit("undoredoChanged", this.undoBuffer, this.redoBuffer);
      this.currentmap.changed = true;
    },

    addRedo(action) {
      this.redoBuffer.push(action);
      this.$emit("undoredoChanged", this.undoBuffer, this.redoBuffer);
      if (this.undoBuffer.length == 0) {
        this.currentmap.changed = false;
      }
    },
    undoImageBuffer() {
      console.log("undo is called");
      if (this.undoStack.length > 0) {
        this.redoStack.push(this.undoStack.pop());
      }
      console.log(this.redoStack);
      if (this.undoStack.length > 0) {
        this.imageContext.drawImage(
          this.undoStack[this.undoStack.length - 1],
          0,
          0
        );
      } else {
        this.imageContext.drawImage(this.originalImageValue, 0, 0);
      }
    },
    redoImageBuffer() {
      if (this.redoStack.length) {
        this.imageContext.drawImage(
          this.redoStack[this.redoStack.length - 1],
          0,
          0
        );
        this.undoStack.push(this.redoStack.pop());
        console.log("Undo", this.undoStack, this.redoStack);
      }
    },
    linkNode(mouseX, mouseY) {
      let objectAtMouse = this.getObjectAtMouse(mouseX, mouseY);
      if (this.arrSelectedObjects.length == 0) {
        if (this.getObjectType(objectAtMouse) == this.$t("common.node"))
          this.selectObject(objectAtMouse, false);
      } else if (this.arrSelectedObjects.length == 1) {
        let fromObject = this.arrSelectedObjects[0];
        if (
          objectAtMouse != null &&
          this.getObjectType(objectAtMouse) == this.$t("common.node")
        ) {
          this.createEdgeLink(fromObject, objectAtMouse, true);
          this.selectObject(objectAtMouse);
        } else {
          this.selectObject(null, false);
        }
      }
      return;
    },
    unlinkNode(mouseX, mouseY) {
      let objectAtMouse = this.getObjectAtMouse(mouseX, mouseY);
      if (this.arrSelectedObjects.length == 0) {
        if (this.getObjectType(objectAtMouse) == this.$t("common.node"))
          this.selectObject(objectAtMouse, false);
      } else if (this.arrSelectedObjects.length == 1) {
        if (
          objectAtMouse != null &&
          this.getObjectType(objectAtMouse) == this.$t("common.node")
        ) {
          let fromObject = this.arrSelectedObjects[0];
          this.deleteEdgeLink(fromObject, objectAtMouse, true);
          this.selectObject(objectAtMouse);
        } else {
          this.selectObject(null, false);
        }
      }
    },
    copyPoi() {
      if (this.arrSelectedObjects.length == 0) return;

      let arrCopy = [];
      for (let object of this.arrSelectedObjects) {
        if (this.getObjectType(object) == this.$t("common.poi")) {
          // Simple Deep copy.
          let newPoi = JSON.parse(JSON.stringify(object));
          newPoi.cpId = "robot-generating_".concat(
            Math.random().toString(36).substring(2, 16)
          );
          newPoi.Position.x += this.getRandomInt(0, 10);
          newPoi.Position.y += this.getRandomInt(0, 10);
          arrCopy.push(newPoi);
        }
      }
      this.addObjects(arrCopy, true);
    },
    copyNode() {
      if (this.arrSelectedObjects.length == 0) return;

      let arrCopy = [];
      for (let object of this.arrSelectedObjects) {
        if (this.getObjectType(object) == this.$t("common.node")) {
          // Simple Deep copy.
          let newNode = JSON.parse(JSON.stringify(object));
          newNode.ID = this.getNewNodeID(object.ID);
          newNode.Position.x += this.getRandomInt(0, 10);
          newNode.Position.y += this.getRandomInt(0, 10);
          newNode.Edge = [];
          arrCopy.push(newNode);
        }
      }
      this.addObjects(arrCopy, true);
    },
    createEdgeLink(fromObject, toObject, addUndo) {
      if (fromObject == null || toObject == null) return;
      console.log("createEdgeLink", fromObject.ID, toObject.ID);

      if (fromObject == toObject) {
        console.log("same error");
        return;
      }
      if (this.getObjectType(fromObject) == this.$t("common.node")) {
        if (fromObject.Edge == null) fromObject.Edge = [];
        if (
          fromObject.Edge.find((edge) => edge.E_Node_Number == toObject.ID) ==
          null
        )
          fromObject.Edge.push({ E_Node_Number: toObject.ID });
        console.log("form object id", toObject.ID);

        if (toObject.Edge == null) toObject.Edge = [];
        if (
          toObject.Edge.find((edge) => edge.E_Node_Number == fromObject.ID) ==
          null
        )
          toObject.Edge.push({ E_Node_Number: fromObject.ID });
        console.log("to object id", fromObject.ID);
        if (addUndo) {
          this.addUndo({
            action: "createEdgeLink",
            fromObject: fromObject,
            toObject: toObject,
          });
        }
      }
    },
    deleteEdgeLink(fromObject, toObject, addUndo, bidirectional) {
      if (bidirectional == undefined) bidirectional = false;
      if (fromObject == null || toObject == null) return;
      if (addUndo == undefined) addUndo = true;
      console.log("deleteEdgeLink", fromObject.ID, toObject.ID);
      if (this.getObjectType(fromObject) == this.$t("common.node")) {
        if (fromObject.Edge != null) {
          fromObject.Edge = fromObject.Edge.filter(
            (edge) => edge.E_Node_Number != toObject.ID
          );
        }

        if (bidirectional == true && toObject.Edge != null) {
          toObject.Edge = toObject.Edge.filter(
            (edge) => edge.E_Node_Number != fromObject.ID
          );
        }
        if (addUndo) {
          this.addUndo({
            action: "deleteEdgeLink",
            fromObject: fromObject,
            toObject: toObject,
          });
        }
      }
    },
    zeroPad(nr, base) {
      if (isNaN(nr)) nr = 1;
      var len = String(base).length - String(nr).length + 1;
      return len > 0 ? new Array(len).join("0") + nr : nr;
    },
    quaternioToAngle(q) {
      if (q == null) return 0;
      let yaw = Math.acos(q.w) * 2;
      if (q.z < 0) yaw = -yaw;
      yaw = (yaw * 180) / Math.PI;
      return yaw;
    },
    pointInsideX1Y1X2Y2(x, y, x1, y1, x2, y2) {
      let minX = Math.min(x1, x2);
      let maxX = Math.max(x1, x2);
      let minY = Math.min(y1, y2);
      let maxY = Math.max(y1, y2);

      let isInside = minX <= x && x <= maxX && minY <= y && y <= maxY;
      return isInside;
    },
    drawNode(p5, node) {
      let screenPosition = this.screenCoordinateFromMapCoordinate(
        node.Position
      );
      p5.strokeWeight(1.5 / this.zoom);
      p5.stroke(0, 0, 0);
      //p5.fill("blue");
      p5.fill("rgba(0, 0, 255, 0.85)");
      let diameter = this.DIAMETER / this.meterForPixel;
      //console.log("diameter of POI",diameter); // Adjusted for zoom
      p5.circle(screenPosition.x, screenPosition.y, diameter);

      // Draw selection circle if the node is selected
      if (this.arrSelectedObjects.indexOf(node) != -1) {
        p5.noStroke();
        p5.fill(128, 0, 0, 255 * 0.3);
        let circleDiameter = (this.DIAMETER + 0.2) / this.meterForPixel;
        p5.circle(screenPosition.x, screenPosition.y, circleDiameter);
      }

      // Draw diameter of the node area if it exists
      if (node.NodeArea && node.NodeArea != 0) {
        let diameter =
          node.NodeArea / this.currentmap.data.MapInfo.MeterForPixel;

        p5.noFill();
        p5.strokeWeight(1);
        p5.stroke(p5.color(255, 255, 0));
        p5.circle(screenPosition.x, screenPosition.y, diameter);
      }
      let angle = 0;
      if (node.Orientation != null) {
        angle = this.quaternioToAngle(node.Orientation);
      }
      p5.push();
      p5.strokeWeight(2 / this.zoom);
      p5.stroke(p5.color(0, 0, 0));
      p5.translate(screenPosition.x, screenPosition.y);
      p5.rotate(angle);
      p5.line(0, 0, (this.DIAMETER + 0.1) / 2 / this.meterForPixel, 0);
      p5.pop();
    },
    displayTooltip(p5, x, y, text, textWidth) {
      let rectStrokeWeight = 1 / this.zoom; // Adjusted for zoom
      p5.stroke(0, 0, 0);
      p5.strokeWeight(rectStrokeWeight);
      p5.drawingContext.setLineDash([0, 0]);
      let textSize = 12 / this.zoom; // Adjusted for zoom
      p5.textSize(textSize);
      p5.fill(255, 255, 255, 255); // Remove the fill to make the border dashed
      p5.rect(x, y - 32 / this.zoom, textWidth + 5, 30 / this.zoom);
      // Draw the text with black color (without dashed border)
      p5.fill(0, 0, 0);
      p5.noStroke(); // Remove the stroke to make the text solid
      p5.push();
      p5.scale(1, -1); // Adjusted for zoom
      p5.text(text, x, -y + 32 / this.zoom); // Adjusted for zoom
      p5.pop();
    },
    drawRotate(p5, item) {
      let w = this.imageRotate.width / this.zoom;
      let h = this.imageRotate.height / this.zoom;

      if (this.store.firstRotate) {
        this.store.elevatorPositionRotate.y += 30 / this.zoom;
        this.store.firstRotate = false;
      }
      p5.push();
      p5.translate(
        this.store.elevatorPositionRotate.x,
        this.store.elevatorPositionRotate.y
      );
      p5.scale(1, -1);
      p5.image(item, 0, 0, w, h);
      p5.pop();

      this.poiElevatorMode(p5);
    },
    poiElevatorMode(p5) {
      p5.textAlign(p5.CENTER, p5.CENTER);
      let textSize = 14 / this.zoom; // Adjusted for zoom
      p5.textSize(textSize);
      let textWidth = p5.textWidth(this.$t("common.ok")) + 20 / this.zoom; // Adjusted for zoom
      this.elevTextWidth = textWidth;
      p5.rectMode(p5.CENTER);
      if (this.store.firstButton) {
        this.store.firstButton = false;
        this.store.elevatorButton.x += 40 / this.zoom;
        this.store.elevatorButton.y += 40 / this.zoom;
      }

      p5.fill(p5.color(255, 255, 255, 255));

      p5.rect(
        this.store.elevatorButton.x,
        this.store.elevatorButton.y,
        textWidth,
        27 / this.zoom,
        1 / this.zoom
      );

      p5.textStyle(p5.BOLD);
      p5.fill(0, 0, 0);
      p5.noStroke();
      p5.push();
      p5.translate(
        this.store.elevatorButton.x,
        this.store.elevatorButton.y - 2 / this.zoom
      );
      p5.scale(1, -1);
      p5.text(this.$t("common.ok"), 0, 0);
      p5.pop();
    },

    rotatePoi(arrElevPoi, mouseX, mouseY, angle, orientation) {
      if (arrElevPoi.length < 4) return;
      if (!this.store.elevatorMode) return;
      let origin = this.mapCoordinateFromScreenCoordinate({
        x: mouseX,
        y: mouseY,
      });

      for (let [index, poi] of arrElevPoi.entries()) {
        if (poi.hasOwnProperty("elevator")) {
          // first two poi clockwise up
          if (index === 0) {
            poi.Position.x = origin.x - 2.0 * Math.cos(angle * (Math.PI / 180)); // root 3 * h/2
            poi.Position.y = origin.y - 2.0 * Math.sin(angle * (Math.PI / 180)); // h/2
          }

          if (index === 1) {
            poi.Position.x = origin.x - 0.7 * Math.cos(angle * (Math.PI / 180));
            poi.Position.y = origin.y - 0.7 * Math.sin(angle * (Math.PI / 180));
          }
          //last two poi clockwise down
          if (index === 2) {
            poi.Position.x = origin.x + 0.7 * Math.cos(angle * (Math.PI / 180));
            poi.Position.y = origin.y + 0.7 * Math.sin(angle * (Math.PI / 180));
          }
          if (index === 3) {
            poi.Position.x = origin.x + 1.4 * Math.cos(angle * (Math.PI / 180));
            poi.Position.y = origin.y + 1.4 * Math.sin(angle * (Math.PI / 180));
          }
        }

        poi.theta =
          orientation == "anticlock" ? (poi.theta += 1) : (poi.theta -= 1);
        if (poi.theta === -180) poi.theta *= -1;
      }
    },

    drawPoi(p5, poi, elev) {
      // Draw Diameter
      let isMouseOver = false;
      let screenPosition = this.screenCoordinateFromMapCoordinate(poi.Position);
      p5.strokeWeight(1.5 / this.zoom);
      p5.stroke(0, 0, 0);
      if (poi.type == 90) {
        //p5.fill("#F17845");
        p5.fill("rgba(255, 140, 0, 0.7)");
      } else if (poi.type == 70 || poi.type == 80) {
        //darkgreen color
        p5.fill("rgba(0, 100, 0, 0.7)");
      } else {
        //this.store.currentChildOrder = 0;
        // Check if the elevatorInOut attribute of the point of interest (poi) is equal to the translated string for "in"
        let isElevatorIn =
          poi.attributes.elevatorInOut ===
          this.$t("option.menu.poi.type.in.out.in");

        // Check if the type of the poi is 50
        let isType50 = poi.type === 50;

        // Define the color red with 70% opacity
        let redColor = "rgba(255, 0, 0, 0.7)";

        // Define the color magenta with 70% opacity
        let magentaColor = "rgba(255, 0, 255, 0.7)";

        // Set the fill color of the p5 object. If the poi is an "in" elevator or its type is 50, set the color to magenta. Otherwise, set it to red.
        p5.fill(isElevatorIn || isType50 ? magentaColor : redColor);
      }

      let diameter = this.DIAMETER / this.meterForPixel;
      //console.log("diameter of POI",diameter); // Adjusted for zoom
      p5.circle(screenPosition.x, screenPosition.y, diameter);
      let menuPosition = p5.createVector(
        screenPosition.x,
        screenPosition.y + 40 / this.zoom // Adjusted for zoom
      );
      // Calculate the rectangle coordinates
      let rectX = menuPosition.x; // Center the rectangle
      let rectY = menuPosition.y - 9 / this.zoom;
      if (this.arrSelectedObjects.indexOf(poi) != -1) {
        p5.noStroke();
        p5.fill(128, 0, 0, 255 * 0.3);
        let circleDiameter = (this.DIAMETER + 0.2) / this.meterForPixel; // Adjusted for zoom
        p5.circle(screenPosition.x, screenPosition.y, circleDiameter);
      }

      if (this.store.showPOIName) {
        p5.textAlign(p5.CENTER, p5.CENTER);
        let textSize = 14 / this.zoom; // Adjusted for zoom
        p5.textSize(textSize);

        // Get the text width
        let textWidth =
          p5.textWidth(getEllipsisText(poi.name["ko-KR"])) + 20 / this.zoom; // Adjusted for zoom

        const dotGap = 2 / this.zoom;
        // Draw the rectangle with the same width as the text
        let rectStrokeWeight = 1.5 / this.zoom; // Adjusted for zoom
        p5.stroke(255, 0, 0);
        p5.strokeWeight(rectStrokeWeight);
        // Apply the line dash pattern only to the border
        p5.drawingContext.setLineDash([dotGap, dotGap]);
        p5.fill(255); // Remove the fill to make the border dashed
        p5.rect(rectX, rectY, textWidth, 40 / this.zoom, 10 / this.zoom);

        p5.fill(0, 0, 0);
        p5.noStroke(); // Remove the stroke to make the text solid
        p5.push();
        p5.scale(1, -1);
        let textY = -rectY; // Adjusted for zoom
        p5.text(getEllipsisText(poi.name["ko-KR"]), rectX, textY); // Adjusted for zoom
        p5.pop();

        let arrowSize = 8 / this.zoom; // Adjusted for zoom
        let spaceBetween = 1 / this.zoom; // Adjusted for zoom
        let triangleY =
          menuPosition.y - 20 / this.zoom - arrowSize - spaceBetween; // Adjusted for zoom

        // Draw the triangle with fixed stroke weight
        let triangleStrokeWeight = 1.5 / this.zoom; // Adjusted for zoom
        p5.drawingContext.setLineDash([]); //POI border will be dashed if you remove this
        p5.push();
        p5.translate(menuPosition.x, triangleY);
        p5.scale(1, -1); // Invert the triangle vertically
        p5.stroke(255, 0, 0);
        p5.strokeWeight(triangleStrokeWeight);
        p5.fill(255); // Remove fill to make the border dotted
        p5.drawingContext.setLineDash([dotGap, dotGap]); // Set the line dash pattern (alternating 5 pixels of line and 5 pixels of space)
        p5.triangle(-arrowSize, 0, arrowSize, 0, 0, arrowSize); // Adjusted for zoom
        p5.pop();
        if (this.mouseMovePosition) {
          let differentialVal = 30;

          let regex = /^[A-Za-z0-9]+$/;

          if (regex.test(poi.name["ko-KR"])) {
            differentialVal = poi.name["ko-KR"].length > 10 ? 55 : 40;
          } else {
            if (poi.name["ko-KR"].length == 1) differentialVal = 20;
            else
              differentialVal =
                poi.name["ko-KR"].length > 10
                  ? 90
                  : poi.name["ko-KR"].length * 10;
          }
          if (
            this.pointInsideRect(
              this.mouseMovePosition.x,
              this.mouseMovePosition.y,
              rectX - differentialVal / this.zoom,
              rectY - 20 / this.zoom,
              textWidth,
              40 / this.zoom
            )
          ) {
            isMouseOver = true;
            this.tempArrPoi = [];
            this.tempArrPoi.push(poi);
          } else {
            isMouseOver = false;
          }
          if (isMouseOver && poi.name["ko-KR"].length > 10) {
            this.tooltipName = poi.name["ko-KR"];
            //if (poi.name["ko-KR"].length > 10)
            this.displayTooltip(
              p5,
              this.mouseMovePosition.x,
              this.mouseMovePosition.y,
              poi.name["ko-KR"],
              p5.textWidth(poi.name["ko-KR"])
            );
          }
        }
      }

      p5.push();
      p5.strokeWeight(2 / this.zoom);
      p5.stroke(p5.color(0, 0, 0));
      p5.translate(screenPosition.x, screenPosition.y);
      p5.rotate(poi.theta);
      if (!(isMouseOver && poi.name["ko-KR"].length > 10))
        p5.line(0, 0, (this.DIAMETER + 0.1) / 2 / this.meterForPixel, 0);
      p5.pop();
    },
    drawEdge(p5, node) {
      if (node.Edge != null && this.store.edgeChecked) {
        for (let aEdge of node.Edge) {
          let aEdgeNode = this.arrNode.find((k) => k.ID == aEdge.E_Node_Number);
          if (aEdgeNode != null && aEdgeNode.Edge != null) {
            let isBidirectional = aEdgeNode.Edge.find(
              (e) => e.E_Node_Number == node.ID
            );

            // Calculate the screen coordinates from map coordinates
            let [screenPosition, screenPositionEdge] = [node, aEdgeNode].map(
              (node) => this.screenCoordinateFromMapCoordinate(node.Position),
              (aEdgeNode) =>
                this.screenCoordinateFromMapCoordinate(aEdgeNode.Position)
            );

            // Calculate the angle between the two nodes
            let angle = Math.atan2(
              aEdgeNode.Position.y - node.Position.y,
              aEdgeNode.Position.x - node.Position.x
            );

            // Set stroke weight and color depending on hilighted edge and bidirectional status
            p5.strokeWeight(aEdge == this.hilightEdge ? 7 : 5 / this.zoom);
            //p5.strokeWeight(aEdge == this.hilightEdge ? 6 : 3);
            p5.stroke(
              isBidirectional ? [14, 105, 7] : [150, 75, 0],
              255 * (aEdge == this.Draw ? 1 : 0)
            );

            // Draw line between nodes
            p5.line(
              screenPosition.x,
              screenPosition.y,
              screenPositionEdge.x,
              screenPositionEdge.y
            );
            let r = (this.DIAMETER + 0.1) / this.meterForPixel / 2;

            //console.log("radius and angle",r,angle)
            // Set fill color depending on bidirectional status
            p5.fill(isBidirectional ? [14, 105, 7] : [150, 75, 0]);

            // Draw triangle to indicate direction of edge
            p5.push();
            p5.translate(screenPositionEdge.x, screenPositionEdge.y);
            p5.rotate((angle * 180) / Math.PI);
            p5.translate(-r, 0); // Translate to center of triangle

            p5.triangle(
              0,
              0,
              (-15 / this.zoom) * this.edgeArrowSize, // variable declared in data() and assign the value 0.4
              (10 / this.zoom) * this.edgeArrowSize,
              (-15 / this.zoom) * this.edgeArrowSize,
              (-10 / this.zoom) * this.edgeArrowSize
            ); // Draw triangle

            // if(this.edgeSelected) {
            //   let boundingBox = this.selectedEdges[0].boundingBox;
            //   let screenCoord = this.screenCoordinateFromMapCoordinate(boundingBox);
            //   this.p5.noStroke();
            //   this.p5.fill(128, 0, 0, 255 * 0.3);
            //   this.p5.rect(screenCoord.x,
            //   screenCoord.y,
            //     boundingBox.width / (this.meterForPixel),
            //     boundingBox.height / (this.meterForPixel));
            // }

            p5.pop(); // Pop matrix stack
          } // End if statement
        } // End for loop
      } // End if statement
    },
    addNode(node) {
      this.currentmap.data.Node.push(node);
    },
    addPoi(poi) {
      this.currentmap.poi.customPointData.push(poi);
    },
    getNewNodeID(lastNodeID) {
      let newNodeId = 0;
      if (lastNodeID != null) {
        let regEx = /\d+$/;
        let n = 1;
        while (true) {
          let newNodeID;
          let result = regEx.exec(lastNodeID);
          console.log("node CNT number: ", result);
          if (result != null) {
            let strNumber = this.zeroPad(parseInt(result[0]) + n, 100);
            newNodeID = lastNodeID.replace(regEx, strNumber);
          } else {
            newNodeID = lastNodeID + this.zeroPad(parseInt(n), 100);
          }
          console.log("newNodeID", newNodeID);
          if (
            this.currentMap.data.Node.find((item) => item.ID == newNodeID) ==
            null
          ) {
            return newNodeID;
          }
          n++;
        }
      }
      return this.nodeNameHeader + "001";
    },
    createNewNode(x, y, z, lastNodeID, index, addUndo) {
      let newNode;
      if (typeof x == "object") {
        newNode = x;
        addUndo = y;
      } else {
        newNode = {
          ID: this.getNewNodeID(lastNodeID),
          // Property: this.selectedNodeProperty, // check!!
          Property: JSON.parse(JSON.stringify(this.defaultAttr)),
          Position: {
            x: x,
            y: y,
            z: z,
          },
          Type: "Node",
          Orientation: {
            w: 1,
            x: 0,
            y: 0,
            z: 0,
          },
          NodeArea: 0.0,
          __index: index,
        };
      }
      return newNode;
    },
    shapeInsideRect(shape, x1, y1, x2, y2) {
      let minX = Math.min(x1, x2);
      let maxX = Math.max(x1, x2);
      let minY = Math.min(y1, y2);
      let maxY = Math.max(y1, y2);

      let shapeMinX, shapeMinY, shapeMaxX, shapeMaxY;
      if (shape.BoundingBox != null) {
        shapeMinX = shape.BoundingBox.x;
        shapeMinY = shape.BoundingBox.y;
        shapeMaxX = shape.BoundingBox.x + shape.BoundingBox.width - 1;
        shapeMaxY = shape.BoundingBox.y + shape.BoundingBox.height - 1;
      } else {
        shapeMinX = shape.Position.x;
        shapeMinY = shape.Position.y;
        shapeMaxX = shape.Position.x + shape.Size.width - 1;
        shapeMaxY = shape.Position.y + shape.Size.height - 1;
      }

      if (
        shapeMaxX / this.meterForPixel <= maxX &&
        shapeMaxY / this.meterForPixel <= maxY &&
        shapeMinX / this.meterForPixel >= minX &&
        shapeMinY / this.meterForPixel >= minY
      )
        return true;

      return false;
    },
    async deleteObjects(objects, addUndo) {
      if (objects == null || objects.length == 0) return;

      for await (const aObject of objects) {
        const type = this.getObjectType(aObject);

        if (type === this.$t("common.node")) {
          this.nodeDelete = true;
          await this.deleteEdgeFromGroupsPostNodeDelete(aObject);
          this.deleteNodeAndLinkedEdges(aObject);
          this.rearrangeIndex("node");
        } else if (type === this.$t("common.poi")) {
          this.deletePoi(aObject);
        } else if (type === this.$t("common.shape")) {
          this.deleteShape(aObject);
          this.rearrangeIndex("shape");
        }
      }

      this.arrSelectedObjects = this.arrSelectedObjects.filter(
        (item) => !objects.includes(item)
      );
      this.$emit("selectedObjectChanged", this.arrSelectedObjects);
      if (addUndo) {
        this.addUndo({
          action: "deleteObjects",
          objects: objects,
        });
      }
    },
    deleteNodeAndLinkedEdges(node) {
      const linkedNodes = this.findNodesIncludeEdge(node.ID);
      if (linkedNodes != null) {
        linkedNodes.forEach((linkedNode) => {
          this.deleteEdgeLink(linkedNode, node, false, true);
        });
      }
      this.deleteNode(node);
    },

    deleteNode(node) {
      this.$emit("clearNodeData");
      let nextIndex = this.listRemove(this.currentMap.data.Node, node);

      return nextIndex;
    },
    deletePoi(poi) {
      this.$emit("clearPoiData");
      this.deleteParentChild(poi);
      let nextIndex = this.listRemove(
        this.currentMap.poi.customPointData,
        poi,
        "poi"
      );
      return nextIndex;
    },
    highlightPoi90(p5) {
      // Object to store the POIs grouped by gid
      let groupedPois = {};

      // Loop through all the POIs
      this.currentMap.poi.customPointData.forEach((poi) => {
        // If the POI type is 70, 80, 90 or 100 or if the POI has a gid
        if ([70, 80, 90, 100].includes(poi.type) && poi.attributes.gid) {
          // If the gid is not yet in the object, add it
          if (!groupedPois[poi.attributes.gid]) {
            groupedPois[poi.attributes.gid] = [];
          }

          // Add the POI to the appropriate group
          groupedPois[poi.attributes.gid].push(poi);
        }
      });

      // Draw lines between the POIs within each group
      Object.values(groupedPois).forEach((poiGroup) => {
        this.drawLinesBetweenPois(poiGroup, p5);
      });
    },

    drawLinesBetweenPois(poiGroup, p5) {
      // Filter POIs of type 70, 80, 90 and sort them by order value
      let pois70_80_90 = poiGroup
        .filter((poi) => [70, 80, 90].includes(poi.type))
        .sort((a, b) => a.attributes.order - b.attributes.order);

      // If there are any POIs of type 70, 80, 90, draw lines from all POIs of type 100 to the one with the lowest order value
      if (pois70_80_90.length > 0) {
        let lowestOrderPoi70_80_90 = pois70_80_90[0];
        poiGroup
          .filter((poi) => poi.type === 100)
          .forEach((poi100) => {
            this.drawGreenLineBetweenPoints(p5, poi100, lowestOrderPoi70_80_90);
          });

        // Draw lines between the remaining POIs of type 70, 80, 90 based on their order
        for (let i = 1; i < pois70_80_90.length; i++) {
          this.drawGreenLineBetweenPoints(
            p5,
            pois70_80_90[i - 1],
            pois70_80_90[i]
          );
        }
      }
    },
    drawGreenLineBetweenPoints(p5, point1, point2) {
      // Calculate the screen coordinates from map coordinates
      let [screenPosition1, screenPosition2] = [point1, point2].map((point) =>
        this.screenCoordinateFromMapCoordinate(point.Position)
      );

      // Calculate the angles between the two points
      let angle1 = Math.atan2(
        screenPosition2.y - screenPosition1.y,
        screenPosition2.x - screenPosition1.x
      );
      let angle2 = Math.atan2(
        screenPosition1.y - screenPosition2.y,
        screenPosition1.x - screenPosition2.x
      );

      // Calculate the new starting and ending positions on the edge of the circles
      let radius =
        this.getValueWithMeterForPixelMultiplier(this.DIAMETER / 2) +
        2 / this.zoom; // Radius of the circle
      let newStartX = screenPosition1.x + radius * Math.cos(angle1);
      let newStartY = screenPosition1.y + radius * Math.sin(angle1);
      let newEndX = screenPosition2.x + radius * Math.cos(angle2);
      let newEndY = screenPosition2.y + radius * Math.sin(angle2);

      // Set stroke weight and color
      p5.strokeWeight(7 / this.zoom);
      p5.stroke(128, 128, 128, 255 * 0.4); // RGB for grey color

      // Draw line from the edge of the first circle to the edge of the second circle
      p5.line(newStartX, newStartY, newEndX, newEndY);
    },

    deleteParentChild(poi) {
      if (poi.type == 90) {
        const count = this.currentmap.poi.customPointData.filter(
          (obj) => obj.attributes.gid === poi.attributes.gid && obj.type == 90
        ).length;
        if (count === 1) {
          // In case of last child, remove only the child
          this.currentmap.poi.customPointData =
            this.currentmap.poi.customPointData.filter(
              (e) => !(e.cpId === poi.cpId && e.type === 90)
            );
          // Find the parent and remove gid, order from parent
          let parent = this.currentmap.poi.customPointData.find(
            (e) =>
              e.attributes.gid === poi.attributes.gid && ![90].includes(e.type)
          );
          // Check if 'parent' is defined
          if (parent) {
            //Iterate over each object in 'arrSelectedObjects'
            this.arrSelectedObjects.forEach((obj) => {
              // If the current object's id matches the parent's id
              if (obj.cpId === parent.cpId) {
                // Remove the 'gid' and 'order' attributes from the current object
                delete obj.attributes.gid;
                delete obj.attributes.order;
              }
            });
            // Remove the 'gid' and 'order' attributes from the parent
            delete parent.attributes.gid;
            delete parent.attributes.order;
            this.currentmap.poi.customPointData.forEach((obj) => {
              if (obj.cpId === parent.cpId) {
                delete obj.attributes.gid;
                delete obj.attributes.order;
              }
            });
          }
          this.$nextTick(() => {
            //this.store.currentChildOrder = 0;
            this.store.setcurrentChildgid("");
            this.store.setcurrentChildOrder(0);
          });
          console.log("currentChildOrder", this.store.currentChildOrder);
        }
      } else if ([70, 80, 100].includes(poi.type) && poi.attributes.gid) {
        // If parent is deleted, remove all children
        this.currentmap.poi.customPointData =
          this.currentmap.poi.customPointData.filter(
            (e) => e.attributes.gid !== poi.attributes.gid || e.type !== 90
          );
      }
    },
    deleteShape(shape) {
      this.$emit("delete");
      return this.listRemove(this.currentMap.data.Shape, shape);
    },
    horizontalAlign() {
      let alignY = null;
      for (let object of this.arrSelectedObjects) {
        if (alignY == null) {
          alignY = object.Position.y;
          break;
        }
      }

      let arrUndoObjects = [];
      for (let object of this.arrSelectedObjects) {
        arrUndoObjects.push({
          prev_x: object.Position.x,
          prev_y: object.Position.y,
          next_x: object.Position.x,
          next_y: alignY,
          node: object,
        });
        object.Position.y = alignY;
      }
      this.addUndo({
        action: "align",
        objects: arrUndoObjects,
      });
    },
    verticalAlign() {
      let alignX = null;
      for (let object of this.arrSelectedObjects) {
        if (alignX == null) {
          alignX = object.Position.x;
          break;
        }
      }
      let arrUndoObjects = [];
      for (let object of this.arrSelectedObjects) {
        arrUndoObjects.push({
          prev_x: object.Position.x,
          prev_y: object.Position.y,
          next_x: alignX,
          next_y: object.Position.y,
          node: object,
        });
        object.Position.x = alignX;
      }
      this.addUndo({
        action: "align",
        objects: arrUndoObjects,
      });
    },
    //write a method to upload large file to s3 Bucket

    equalSpace() {
      if (this.arrSelectedObjects.length < 2) return;
      let firstObject = this.arrSelectedObjects[0];
      let lastObject =
        this.arrSelectedObjects[this.arrSelectedObjects.length - 1];

      let space_x =
        (lastObject.Position.x - firstObject.Position.x) /
        (this.arrSelectedObjects.length - 1);
      let space_y =
        (lastObject.Position.y - firstObject.Position.y) /
        (this.arrSelectedObjects.length - 1);

      let arrUndoObjects = [];
      for (let i = 1; i < this.arrSelectedObjects.length - 1; i++) {
        let object = this.arrSelectedObjects[i];
        arrUndoObjects.push({
          prev_x: object.Position.x,
          prev_y: object.Position.y,
          next_x: firstObject.Position.x + i * space_x,
          next_y: firstObject.Position.y + i * space_y,
          node: object,
        });
        object.Position.x = firstObject.Position.x + i * space_x;
        object.Position.y = firstObject.Position.y + i * space_y;
      }
      this.addUndo({
        action: "align",
        objects: arrUndoObjects,
      });
    },

    findNodesIncludeEdge(strID) {
      let aLinkedNodes = [];
      for (let aNode of this.currentMap.data.Node) {
        if (aNode.Edge != null) {
          for (let aEdge of aNode.Edge) {
            if (aEdge.E_Node_Number == strID) {
              aLinkedNodes.push(aNode);
              break;
            }
          }
        }
      }
      return aLinkedNodes;
    },

    listRemove(list, item, val) {
      let index = list.indexOf(item);

      if (index > -1) list.splice(index, 1);
      if (val === "poi") {
        if (index > -1) this.arrPoi.splice(index, 1);
        this.tempArrPoi = [];
      }
      if (index >= list.length) return list.length - 1;

      return index;
    },
    checkMultiSelect() {
      if (this.isEdgeSettingModeEnabled) return;
      if (this.guideLine == null) return;
      this.selectObject(null, false);

      for (let node of this.currentMap.data.Node) {
        let screeenPosition = this.screenCoordinateFromMapCoordinate(
          node.Position
        );
        if (
          this.pointInsideX1Y1X2Y2(
            screeenPosition.x,
            screeenPosition.y,
            this.guideLine.x,
            this.guideLine.y,
            this.guideLine.x2,
            this.guideLine.y2
          )
        ) {
          this.selectObject(node, true);
        }
      }
      for (let poi of this.currentMap.poi.customPointData) {
        let screeenPosition = this.screenCoordinateFromMapCoordinate(
          poi.Position
        );
        if (
          this.pointInsideX1Y1X2Y2(
            screeenPosition.x,
            screeenPosition.y,
            this.guideLine.x,
            this.guideLine.y,
            this.guideLine.x2,
            this.guideLine.y2
          )
        ) {
          this.selectObject(poi, true);
        }
      }

      for (let shape of this.currentMap.data.Shape) {
        if (
          this.shapeInsideRect(
            shape,
            this.guideLine.x,
            this.guideLine.y,
            this.guideLine.x2,
            this.guideLine.y2
          )
        ) {
          this.selectObject(shape, true);
        }
      }
      //this.guideLine = null;
    },
    drawGuideLine(p5) {
      if (this.guideLine == null) return;

      let circleRadius = 1 / this.zoom; // Adjust the circle radius as needed
      let rectangleSize = 15 / this.zoom; // Adjust the rectangle size as needed

      if (this.guideLine.drawMeter) {
        // Guidelines When 100%, it should be given the same size.
        if (["menuPencil", "menuRuler"].includes(this.subMenuSelection)) {
          p5.strokeWeight(1 / this.zoom);
        } else {
          p5.strokeWeight(1);
        }

        p5.stroke(this.colorGuideLine);
        p5.noFill();
        p5.line(
          this.guideLine.x,
          this.guideLine.y,
          this.guideLine.x2,
          this.guideLine.y2
        );

        // Draw filled circles on both sides of the line
        p5.fill(255, 0, 0); // Red color for circles
        p5.ellipse(this.guideLine.x, this.guideLine.y, circleRadius * 2);
        p5.ellipse(this.guideLine.x2, this.guideLine.y2, circleRadius * 2);
        let rectCenterX = (this.guideLine.x + this.guideLine.x2) / 2;
        let rectCenterY = (this.guideLine.y + this.guideLine.y2) / 2 + 1;
        if (this.subMenuSelection === "menuPencil") {
          // Draw rectangles with a red border

          p5.strokeWeight(1 / this.zoom);
          p5.stroke("#A50034");
          p5.noFill();
          p5.rect(
            this.guideLine.x,
            this.guideLine.y,
            this.pencilSize,
            this.pencilSize
          );
          p5.rect(
            this.guideLine.x2,
            this.guideLine.y2,
            this.pencilSize,
            this.pencilSize
          );
        }

        let distance =
          this.getDistance(
            this.guideLine.x,
            this.guideLine.y,
            this.guideLine.x2,
            this.guideLine.y2
          ) * this.currentmap.data.MapInfo.MeterForPixel;
        let boxWidth = 50 / this.zoom;
        let boxHeight = 20 / this.zoom;
        p5.strokeWeight(1 / this.zoom);
        p5.stroke(100);
        p5.fill(p5.color(255, 255, 255, 255));
        p5.rect(rectCenterX, rectCenterY, boxWidth, boxHeight);
        p5.noStroke();
        p5.fill(0, 0, 0);
        p5.push();
        p5.translate(rectCenterX, rectCenterY);
        p5.scale(1 / this.zoom, -1 / this.zoom);
        p5.text(distance.toFixed(1) + "m", 0, 0);
        p5.pop();
      } else {
        this.prepareDash(p5);
        p5.strokeWeight(2 / this.zoom);
        p5.stroke(255, 0, 0, 255 * 0.5);
        p5.noFill();

        let rectCenterX = (this.guideLine.x + this.guideLine.x2) / 2;
        let rectCenterY = (this.guideLine.y + this.guideLine.y2) / 2;

        // Draw filled circles on both sides of the line
        p5.fill(255, 0, 0); // Red color for circles
        p5.ellipse(this.guideLine.x, this.guideLine.y, circleRadius * 2);
        p5.ellipse(this.guideLine.x2, this.guideLine.y2, circleRadius * 2);

        // Draw rectangles with a red border
        p5.strokeWeight(1 / this.zoom);
        p5.stroke(255, 0, 0); // Red border color for rectangles
        p5.noFill();
        p5.rect(
          rectCenterX,
          rectCenterY,
          Math.abs(this.guideLine.x - this.guideLine.x2),
          Math.abs(this.guideLine.y - this.guideLine.y2)
        );

        this.finishDash(p5);
      }
    },

    // drawGuideLine(p5) {
    //   if (this.guideLine == null) return;

    //   if (this.guideLine.drawMeter) {
    //     //Guidelines When 100%, it should be given the same size.
    //     if (["menuPencil", "menuRuler"].includes(this.subMenuSelection)) {
    //       p5.strokeWeight(1 / this.zoom);
    //     } else {
    //       p5.strokeWeight(1);
    //     }

    //     p5.stroke(this.colorGuideLine);
    //     p5.noFill();
    //     p5.line(
    //       this.guideLine.x,
    //       this.guideLine.y,
    //       this.guideLine.x2,
    //       this.guideLine.y2
    //     );
    //     let distance =
    //       this.getDistance(
    //         this.guideLine.x,
    //         this.guideLine.y,
    //         this.guideLine.x2,
    //         this.guideLine.y2
    //       ) * this.currentmap.data.MapInfo.MeterForPixel; //this.currentmap.data.MapInfo.MeterForPixel;
    //     let center_x = (this.guideLine.x + this.guideLine.x2) / 2;
    //     let center_y =
    //       (this.guideLine.y + 20 / this.zoom + this.guideLine.y2) / 2;
    //     let boxWidth = 50,
    //       boxHeight = 20;
    //     p5.strokeWeight(1 / this.zoom);
    //     p5.stroke(100); // this.zoom;
    //     p5.fill(p5.color(255, 255, 255, 255));
    //     p5.rect(
    //       center_x,
    //       center_y,
    //       boxWidth / this.zoom,
    //       boxHeight / this.zoom
    //     );
    //     p5.noStroke();
    //     p5.fill(0, 0, 0);
    //     p5.push();
    //     p5.translate(center_x, center_y);
    //     p5.scale(1 / this.zoom, -1 / this.zoom);
    //     p5.text(distance.toFixed(1) + "m", 0, 0);
    //     p5.pop();
    //   } else {
    //     this.prepareDash(p5);
    //     p5.strokeWeight(2 / this.zoom);
    //     p5.stroke(255, 0, 0, 255 * 0.5);
    //     p5.noFill();

    //     let center_x = (this.guideLine.x + this.guideLine.x2) / 2;
    //     let center_y = (this.guideLine.y + this.guideLine.y2) / 2;
    //     p5.rect(
    //       center_x,
    //       center_y,
    //       Math.abs(this.guideLine.x - this.guideLine.x2),
    //       Math.abs(this.guideLine.y - this.guideLine.y2)
    //     );
    //     this.finishDash(p5);
    //   }
    // },
    drawHandle(p5) {
      for (let handle of this.arrHandles) {
        if (handle.target.Orientation != null && handle.target.Orientation != 0)
          return;
        for (let object of this.arrSelectedObjects) {
          if (
            this.getObjectType(object) == this.$t("common.shape") &&
            handle.target == object
          ) {
            if (
              object.shape_type == "rectangle" ||
              object.shape_type == "ellipse" ||
              object.shape_type == "triangle"
            ) {
              let newPos = this.getShapeBoundingPoint(
                object,
                handle.type,
                "test"
              );
              if (newPos != null) {
                handle.x = newPos.x;
                handle.y = newPos.y;
              }
            } else {
              handle.x = handle.targetPoint.x;
              handle.y = handle.targetPoint.y;
            }
          }
        }

        p5.stroke(0);
        p5.strokeWeight(1);
        if (this.selectedHandle == handle) p5.fill(255, 0, 0);
        else p5.fill(255);
        let screenPosition = this.screenCoordinateFromMapCoordinate(handle);
        p5.circle(
          screenPosition.x,
          screenPosition.y,
          HANDLE_RADIUS / this.zoom,
          HANDLE_RADIUS / this.zoom
        );
      }
    },
    onDragHandle(handle, mouseX, mouseY) {
      let shape = handle.target;
      console.log("odh----", handle.type, mouseX, mouseY);
      let mapPosition = this.mapCoordinateFromScreenCoordinate({
        //rain:
        x: mouseX,
        y: mouseY,
      });

      let orientation = (shape.Orientation * Math.PI) / 180;
      if (shape.shape_type == "line" || shape.shape_type == "polygon") {
        handle.targetPoint.x = mapPosition.x;
        handle.targetPoint.y = mapPosition.y;
        handle.x = mapPosition.x;
        handle.y = mapPosition.y;
        this.calcBoundingBox(shape);
      } else {
        // 0도가 아닌 회전 상태에서는 드래그 변경을 막음. 0도로 바꾸고 다시 변경하도록 요청
        let screenPosition = this.screenCoordinateFromMapCoordinate(
          shape.Position
        );
        if (orientation != 0) return;
        if (handle.type == "bottomLeft") {
          shape.Size.width -= (mouseX - screenPosition.x) * this.meterForPixel;
          shape.Size.height -= (mouseY - screenPosition.y) * this.meterForPixel;
          shape.Position.x = mapPosition.x;
          shape.Position.y = mapPosition.y;
        } else if (handle.type == "bottomRight") {
          shape.Size.width = (mouseX - screenPosition.x) * this.meterForPixel;
          shape.Size.height -= (mouseY - screenPosition.y) * this.meterForPixel;
          shape.Position.y = mapPosition.y;
        } else if (handle.type == "topRight") {
          shape.Size.width = (mouseX - screenPosition.x) * this.meterForPixel;
          shape.Size.height = (mouseY - screenPosition.y) * this.meterForPixel;
        } else if (handle.type == "topLeft") {
          shape.Size.width -= (mouseX - screenPosition.x) * this.meterForPixel;
          shape.Size.height = (mouseY - screenPosition.y) * this.meterForPixel;
          shape.Position.x = mapPosition.x;
        } else {
          let kittyPoint = this.getShapeBoundingPoint(shape, "bottomLeft");
          let center = this.getCenter(
            mouseX,
            mouseY,
            kittyPoint.x,
            kittyPoint.y
          );
          let theta =
            Math.atan2(mouseY - kittyPoint.y, mouseX - kittyPoint.x) +
            orientation;
          let r = this.getDistance(kittyPoint.x, kittyPoint.y, mouseX, mouseY);
          shape.Size.width = r * Math.cos(theta);
          shape.Size.height = r * Math.sin(theta);
          shape.Position.x = Math.ceil(center.x - (r / 2) * Math.cos(theta));
          shape.Position.y = Math.ceil(center.y - (r / 2) * Math.sin(theta));
        }
      }
    },
    calcBoundingBox(shape) {
      let minX, minY, maxX, maxY;
      if (shape.shape_type === "line" || shape.shape_type === "polygon") {
        for (let point of shape.Position) {
          if (minX == null) minX = point.x;
          if (minY == null) minY = point.y;
          if (maxX == null) maxX = point.x;
          if (maxY == null) maxY = point.y;
          if (point.x < minX) minX = point.x;
          if (point.x > maxX) maxX = point.x;
          if (point.y < minY) minY = point.y;
          if (point.y > maxY) maxY = point.y;
        }
        shape.BoundingBox = {
          x: minX,
          y: minY,
          width: maxX - minX + 1,
          height: maxY - minY + 1,
        };
      }
    },
    finishLineDraw() {
      //this.checkdismiss();

      if (this.drawingShape != null) {
        if (this.drawingShape.shape_type === "polygon") {
          if (this.drawingShape.Position.length < 3) {
            this.drawingShape.Size = null;
            this.drawingShape = null;
            this.$emit("backToMenuSelect");
            this.guideLine = null;
            return;
          }
        }
        delete this.drawingShape.isDrawing;
        this.calcBoundingBox(this.drawingShape);
        this.addObjects([this.drawingShape], true);
        this.drawingShape.Size = null;
        this.drawingShape = null;
      }

      this.guideLine = null;
    },
    getHandleAtMouse(x, y) {
      for (let handle of this.arrHandles) {
        let screenPosition = this.screenCoordinateFromMapCoordinate(handle);
        if (
          this.getDistance(screenPosition.x, screenPosition.y, x, y) <
          HANDLE_RADIUS / this.zoom
        ) {
          console.log("getHandleAtMouse", handle.type, x, y);
          return handle;
        }
      }
      return null;
    },
    getObjectAtMouse(x, y) {
      if (this.store.POIChecked) {
        for (let i = this.arrPoi.length - 1; i >= 0; i--) {
          let poi = this.arrPoi[i];
          let poiRaidius = this.getValueWithMeterForPixelMultiplier(
            this.DIAMETER / 2
          );
          let screenPosition = this.screenCoordinateFromMapCoordinate(
            poi.Position
          );
          if (
            this.pointInsideRect(
              x,
              y,
              screenPosition.x - poiRaidius,
              screenPosition.y - poiRaidius,
              poiRaidius * 2,
              poiRaidius * 2
            )
          ) {
            return poi;
          }
        }
      }
      if (this.store.nodeChecked) {
        for (let i = this.arrNode.length - 1; i >= 0; i--) {
          let node = this.arrNode[i];
          let nodeRaidius = this.getValueWithMeterForPixelMultiplier(
            this.DIAMETER / 2
          );
          let screenPosition = this.screenCoordinateFromMapCoordinate(
            node.Position
          );
          if (
            this.pointInsideRect(
              x,
              y,
              screenPosition.x - nodeRaidius,
              screenPosition.y - nodeRaidius,
              nodeRaidius * 2,
              nodeRaidius * 2
            )
          ) {
            return node;
          }
        }
      }
      if (this.store.shapeChecked) {
        for (let i = this.arrShape.length - 1; i >= 0; i--) {
          let shape = this.arrShape[i];
          if (shape.BoundingBox != null) {
            let screenPosition = this.screenCoordinateFromMapCoordinate(
              shape.BoundingBox
            );
            if (
              this.pointInsideRect(
                x,
                y,
                screenPosition.x,
                screenPosition.y,
                shape.BoundingBox.width / this.meterForPixel,
                shape.BoundingBox.height / this.meterForPixel
              )
            ) {
              return shape;
            }
          } else {
            let screenPosition = this.screenCoordinateFromMapCoordinate(
              shape.Position
            );
            if (
              this.pointInsideRect(
                x,
                y,
                screenPosition.x,
                screenPosition.y,
                shape.Size.width / this.meterForPixel,
                shape.Size.height / this.meterForPixel
              )
            ) {
              return shape;
            }
          }
        }
      }
      return null;
    },
    /**
     * Function to get the type of an object based on a given value.
     * @param {string} val - The input value.
     * @returns {string} The type of the object.
     */
    getTypeOfObj(val) {
      // Define a map that associates input values with their corresponding types
      const typeMap = {
        [this.$t("common.shape")]: "shape",
        [this.$t("common.node")]: "node",
        [this.$t("common.poi")]: "POI",
      };

      // Return the type corresponding to the input value, or the input value itself if it's not found in the map
      return typeMap[val] || val;
    },
    selectObject(object, isMultiSelect) {
      this.isSelectingObject = true;
      if (isMultiSelect == undefined) isMultiSelect = false;
      console.log("selectObject", object, isMultiSelect);
      if (object === null) {
        this.store.drawer = false;

        this.store.optionPanelColor = "";
        if (this.store.escTriger === false) {
          this.$emit("currentObj", object);
        }
      }
      if (this.getObjectType(object) === this.$t("common.shape"))
        this.store.optionPanelColor = object.color;
      this.store.setOptionNodeValue(object);
      this.arrHandles = [];

      if (object == null) {
        this.arrSelectedObjects = [];
      } else {
        // Check if the corresponding 'Checked' property in the store is true
        let objectType = this.getObjectType(object);
        console.log("objectType11", objectType);
        let storeProperty = this.getTypeOfObj(objectType);
        console.log("objectType12", objectType);
        let isObjectChecked = this.store[storeProperty + "Checked"];
        if (isObjectChecked) {
          let index = this.arrSelectedObjects.indexOf(object);
          let storeSelection = [];
          if (isMultiSelect) {
            if (index === -1) {
              this.arrSelectedObjects.push(object);
            } else {
              this.arrSelectedObjects.splice(index, 1);
            }
            console.log("multiselect", this.arrSelectedObjects.length);
            this.arrSelectedObjects.map((item) => {
              storeSelection.push(this.getObjectType(item));
            });
            let uniqueArray = Array.from(new Set(storeSelection));
            if (uniqueArray.length > 1)
              this.store.selectedProperty = this.$t("common.shape");
          } else {
            if (
              this.arrSelectedObjects.length == 1 &&
              this.draggingObject.length > 0
            ) {
              let lastObject = this.arrSelectedObjects[0];
            }
            if (index == -1) this.arrSelectedObjects = [object];
          }
          console.log("selectObject", this.arrSelectedObjects.length);
          if (this.store.optionChecked) this.store.drawer = true;
          if (this.arrSelectedObjects.length === 1) {
            console.log(
              "this.arrSelectedObjects.length",
              this.arrSelectedObjects.length
            );
            this.selectedProperty = this.getObjectType(object);
            this.store.selectedProperty = this.selectedProperty;
            console.log("result", this.selectedProperty);
            if (["Node", "Poi"].includes(this.selectedProperty))
              this.store.optionPanelColor = "";
            if (this.store.escTriger === false) {
              this.$emit("currentObj", this.selectedProperty);
            }
          }

          if (this.arrSelectedObjects.length === 0) {
            this.store.setOptionNodeValue(null);
          }
        }

        // TODO:
        this.$emit("selectedObjectChanged", this.arrSelectedObjects);

        console.log("multiselected objects", this.arrSelectedObjects);
        this.initialPOIPos.clear();
        for (const element of this.arrSelectedObjects) {
          if (element.hasOwnProperty("cpId")) {
            this.initialPOIPos.set(
              element.cpId,
              Object.assign({}, element.Position)
            );
          }
        }
      }
    },
    addShape(shape) {
      this.currentmap.data.Shape.push(shape);
      // Sort shapes based on their priority in the colorPriority map
      const colorPriority = new Map([
        ["#FF0000", 1], //Red
        ["#0000FF", 2], //Blue
        ["#00FF00", 3], //Green
        ["#FF00FF", 4], //Purple
        ["#FFFF00", 5], //Yellow
        ["#FFA500", 5], //Orange
        // Add more colors as needed
      ]);

      this.currentmap.data.Shape.sort((a, b) => {
        const priorityA = colorPriority.get(a.color) || Number.MAX_VALUE; // Default to a low priority if color not found

        const priorityB = colorPriority.get(b.color) || Number.MAX_VALUE; // Default to a low priority if color not found
        console.log("priorityA", priorityA);
        console.log("priorityB", priorityB);
        return priorityB - priorityA;
      });
    },
    getObjectType(object) {
      if (object == null) {
        console.log("getObjectType Error object null");
        return null;
      }
      if (object.hasOwnProperty("shape_type")) return this.$t("common.shape");
      if (object.hasOwnProperty("cpId")) return this.$t("common.poi");
      return this.$t("common.node");
    },
    addObjects(objects, addUndo) {
      console.log("POI Line-up testing", objects);
      for (let aObject of objects) {
        let type = this.getObjectType(aObject);
        if (type == this.$t("common.node")) {
          console.log(aObject);
          this.addNode(aObject);
        } else if (type == this.$t("common.poi")) {
          this.addPoi(aObject);
        } else if (type === this.$t("common.shape")) {
          this.addShape(aObject);
        }
      }
      if (addUndo) {
        this.addUndo({
          action: "addObjects",
          objects: objects,
        });
      }
    },
    getLastNodeID() {
      if (this.arrNode.length > 0)
        return this.arrNode[this.arrNode.length - 1].ID;
      return null;
    },
    newNode(mouseX, mouseY) {
      let mapPosition = this.mapCoordinateFromScreenCoordinate({
        x: mouseX,
        y: mouseY,
      });
      let newNode = this.createNewNode(
        mapPosition.x,
        mapPosition.y,
        this.mapInfoZPosition,
        this.getLastNodeID(),
        this.currentmap.data.Node.length
      );
      this.addObjects([newNode], true);
      this.selectObject(newNode, true);
      if (this.lastNewNode == null) {
        this.lastNewNode = newNode;
      } else {
        this.lastNewNode = newNode;
      }
    },
    newPoi(mouseX, mouseY, p5) {
      console.log("newNPoi");
      let mapPosition = this.mapCoordinateFromScreenCoordinate({
        x: mouseX,
        y: mouseY,
      });

      // This is for menu elevator
      if (
        ["menuElevator"].includes(this.subMenuSelection) &&
        !this.arrSelectedObjects.length
      ) {
        this.menuElevatorFunction(
          mapPosition,
          mouseX,
          mouseY,
          p5,
          `elevator-${new Date().getTime()}`
        );
        this.store.elevatorPosition.x = mouseX;
        this.store.elevatorPosition.y = mouseY;
        this.store.elevatorPositionRotate.x = mouseX;
        this.store.elevatorPositionRotate.y = mouseY;
        this.store.elevatorButton.x = mouseX;
        this.store.elevatorButton.y = mouseY;
      }

      if (this.subMenuSelection !== "menuElevator") {
        this.getPixelValAtPos(
          Math.floor(mouseX),
          this.mapImage.height - Math.floor(mouseY),
          this.imageContextForValidation
        ); // change 2000 to this.mapImage.height
        this.getShapeDataAtPos(
          Math.floor(mouseX),
          this.mapImage.height - Math.floor(mouseY),
          this.shapeContextForValidation
        );
        if (
          this.combinedPixelData === 0 ||
          this.combinedPixelData === 450 ||
          this.combinedShapeData == "00255"
        ) {
          // console.warn("NOT CREATING POI!!");
          return;
        }
      }
      // console.warn("CREATING NEW POI!!");
      // this is for creating single poi
      if (this.subMenuSelection !== "menuElevator") {
        let newPoi = this.createNewPoi(
          mapPosition.x,
          mapPosition.y,
          this.mapInfoZPosition
        );

        this.addObjects([newPoi], true);
        console.log(newPoi.ID);

        this.selectObject(newPoi, true);
        if (this.lastNewPoi == null) {
          this.lastNewPoi = newPoi;
        } else {
          this.lastNewPoi = newPoi;
        }
      }
    },

    menuElevatorFunction(mapPosition, mouseX, mouseY, p5, elevName) {
      let newPoi;
      let numberOfPoi = 4;
      let tempArr = [];
      for (let index = 0; index < numberOfPoi; index++) {
        newPoi = "";

        newPoi = this.createNewPoi(
          mapPosition.x,
          mapPosition.y,
          this.mapInfoZPosition,
          index,
          elevName
        );

        tempArr.push(newPoi);
        this.selectObject(newPoi, true);

        if (this.lastNewPoi == null) {
          this.lastNewPoi = newPoi;
        } else {
          this.lastNewPoi = newPoi;
        }
      }
      for (let index = 0; index < tempArr.length; index++) {
        this.addObjects([tempArr[index]], true);
      }
      this.elevatorPoi.push(tempArr);
      this.store.elevatorMode = true;
      if (this.store.elevatorMode && this.rotateArray.length === 0) {
        this.rotateArray.push(this.imageRotate);
      }
    },

    createNewPoi(x, y, z, index, elevName) {
      let newPoi = {
        name: {
          en: "",
          "en-US": "",
          kr: "",
          "ko-KR": "",
        },
        cpId: "robot-generating_".concat(
          Math.random().toString(36).substring(2, 16)
        ),
        Position: {
          x: x,
          y: y,
        },
        attributes: {
          callbell: "",
          desc: "",
          tel: "",
          buildingTransferPoint: {
            destinationMap: "",
            x: 0,
            y: 0,
            z: 0,
            theta: 0,
          },
          elevatorID: "",
          elevatorVendor: "",
          elevatorFloor: "",
          elevatorFloorList: "",
          elevatorDoor: "",
          elevatorInOut: "",
          macAddress: "",
          companyName: "",
        },
        isRestricted: 0,
        radius: 3,
        theta: 0,
        type: 100,
      };
      if (this.store.showPOILineUp) newPoi.attributes.range = 0.0;
      // this code for autocreate poi
      if (this.store.autoCreateBool) {
        newPoi.name.en =
          newPoi.name["en-US"] =
          newPoi.name.kr =
          newPoi.name["ko-KR"] =
            this.gettingPoiNameNumber();
      }

      // this code for elevator poi
      if (["menuElevator"].includes(this.subMenuSelection)) {
        newPoi.elevator = elevName;
        newPoi.type = 3;
        newPoi.radius = 0; //assign poi radius value
        // Assigning REIS api values
        const {
          elevatorId,
          elevatorVendor,
          elevatorFloor,
          elevatorFloorList,
          elevatorDoor,
        } = this.store.reisData;
        newPoi.attributes.elevatorDoor = elevatorDoor;
        newPoi.attributes.elevatorFloor = elevatorFloor;
        newPoi.attributes.elevatorFloorList = elevatorFloorList;
        newPoi.attributes.elevatorVendor = elevatorVendor;
        newPoi.attributes.elevatorID = elevatorId;
        if (index === 0) {
          newPoi.Position.x -= this.addingValue * 2 + 0.6;
          newPoi.attributes.elevatorInOut = this.$t(
            "option.menu.poi.type.in.out.out"
          );
          newPoi.name.en =
            newPoi.name["en-US"] =
            newPoi.name.kr =
            newPoi.name["ko-KR"] =
              "EV_OUT";
        }
        if (index === 1) {
          newPoi.Position.x -= this.addingValue;
          newPoi.attributes.elevatorInOut = this.$t(
            "option.menu.poi.type.in.out.sensing"
          );
          newPoi.name.en =
            newPoi.name["en-US"] =
            newPoi.name.kr =
            newPoi.name["ko-KR"] =
              "EV_OUTSENSING";
        }
        if (index === 2) {
          newPoi.Position.x += this.addingValue;
          newPoi.attributes.elevatorInOut = this.$t(
            "option.menu.poi.type.in.out.in.sensing"
          );
          newPoi.name.en =
            newPoi.name["en-US"] =
            newPoi.name.kr =
            newPoi.name["ko-KR"] =
              "EV_INSENSING";
        }
        if (index === 3) {
          newPoi.Position.x += this.addingValue * 2;
          newPoi.attributes.elevatorInOut = this.$t(
            "option.menu.poi.type.in.out.in"
          );
          newPoi.name.en =
            newPoi.name["en-US"] =
            newPoi.name.kr =
            newPoi.name["ko-KR"] =
              "EV_IN";
        }
        if (index > 1) newPoi.theta = 180;
      }

      if (this.store.createPoiLineUp) {
        const len = this.currentMap.poi.customPointData.findIndex(
          (e) =>
            e.cpId ===
            this.arrSelectedObjects[this.arrSelectedObjects.length - 1].cpId
        );
        let poiObj = this.currentMap.poi.customPointData[len];
        console.log("Parent POI Obj", poiObj);
        let currentChildId = this.store.currentChildgid;
        if (!poiObj.attributes.gid) {
          poiObj.attributes.gid = this.store.currentChildgid;
          poiObj.attributes.order = 0;
          if (this.store.showPOILineUp) poiObj.attributes.range = 0.0;
          this.currentmap.poi.customPointData.splice(len, 1, poiObj);
        } else {
          currentChildId = poiObj.attributes.gid;
        }
        // Get the maximum order value for the current gid
        let maxOrder = this.maxOrderValues[currentChildId] || 0;
        console.log("maxOrder", maxOrder);
        newPoi.type = 90;
        newPoi.attributes.gid = currentChildId;
        newPoi.attributes.order = maxOrder + 1; // Increment the maximum order value
        if (this.store.showPOILineUp) newPoi.attributes.range = 0.0;
        // /If the Parent's name is set before pressing the '+' button, automatically generate the Child's name.
        //The format is ‘Parent POI Name - order’
        newPoi.name.en = poiObj.name.en
          ? poiObj.name.en.split("-")[0].trim() + "-" + (maxOrder + 1)
          : "";
        newPoi.name["en-US"] = poiObj.name["en-US"]
          ? poiObj.name["en-US"].split("-")[0].trim() + "-" + (maxOrder + 1)
          : "";
        newPoi.name.kr = poiObj.name.kr
          ? poiObj.name.kr.split("-")[0].trim() + "-" + (maxOrder + 1)
          : "";
        newPoi.name["ko-KR"] = poiObj.name["ko-KR"]
          ? poiObj.name["ko-KR"].split("-")[0].trim() + "-" + (maxOrder + 1)
          : "";
        // Update the maximum order value for the current gid
        this.maxOrderValues[currentChildId] = maxOrder + 1;
      }

      return newPoi;
    },
    gettingPoiNameNumber(getFlag) {
      //Creating Increment of POI name
      let splitValue = this.store.autoCreateBool
        ? this.store.autoCreatePoi.acPoiNo.split("")
        : this.store.multiplePoi.multiNumber.split("");
      splitValue = splitValue.map((i) => Number(i)); //convert string to number
      const isAllZero = splitValue.every((item) => item === 0); // checking all values are zero
      this.getZero = "";
      let setFlag = false;

      if (!this.store.firstTime) {
        splitValue.forEach((item) => {
          if (item > 0) setFlag = true;
          if (!setFlag && splitValue.length > 1) {
            if (["0", 0].includes(item)) this.getZero += item;
          }
        });
        if (this.firstFlag && isAllZero) {
          this.getZero = this.getZero.substring(1);
          this.firstFlag = false;
        }
      } else {
        this.firstFlag = true;
      }
      if (getFlag) this.store.firstTime = true;
      let nextValue = this.store.firstTime
        ? Number(
            this.store.autoCreateBool
              ? this.store.autoCreatePoi.acPoiNo
              : this.store.multiplePoi.multiNumber
          )
        : Number(
            this.store.autoCreateBool
              ? this.store.autoCreatePoi.acPoiNo
              : this.store.multiplePoi.multiNumber
          ) + 1;

      if (this.store.autoCreateBool)
        this.store.autoCreatePoi.acPoiNo = this.store.firstTime
          ? this.store.autoCreatePoi.acPoiNo
          : `${this.getZero}${nextValue}`;
      if (!this.store.autoCreateBool)
        this.store.multiplePoi.multiNumber = this.store.firstTime
          ? this.store.multiplePoi.multiNumber
          : `${this.getZero}${nextValue}`;

      if (this.store.firstTime) {
        this.store.firstTime = false;
        return this.store.autoCreateBool
          ? `${this.store.autoCreatePoi.acPoiName}${this.store.autoCreatePoi.acPoiNo}`
          : `${this.store.multiplePoi.multiName}${this.store.multiplePoi.multiNumber}`;
      } else {
        return this.store.autoCreateBool
          ? `${this.store.autoCreatePoi.acPoiName}${this.getZero}${nextValue}`
          : `${this.store.multiplePoi.multiName}${this.getZero}${nextValue}`;
      }
    },
    stopNewNode() {
      this.lastNewNode = null;
      let firstNode = this.arrSelectedObjects[0];
      this.selectObject(null, false);
      this.selectObject(firstNode, false);
      this.$emit("backToMenuSelect");
    },
    stopNewPoi() {
      this.lastNewPoi = null;
      let firstPoi = this.arrSelectedObjects[0];
      this.selectObject(null, false);
      this.selectObject(firstPoi, false);
      this.$emit("backToMenuSelect");
    },

    getShapeBoundingPoint(shape, position, reason) {
      if (shape == null) return;
      if (shape.Orientation == null) {
        shape.Orientation = 0;
      }
      let center_x = shape.Position.x + shape.Size.width / 2;
      let center_y = shape.Position.y + shape.Size.height / 2;
      let r = Math.sqrt(
        ((shape.Size.width / 2) * shape.Size.width) / 2 +
          ((shape.Size.height / 2) * shape.Size.height) / 2
      );
      if (position == "bottomLeft") {
        let theta = Math.atan2(shape.Size.height, -shape.Size.width);
        let rotation = theta - (shape.Orientation * Math.PI) / 180;
        return {
          x: center_x + r * Math.cos(rotation),
          y: center_y - r * Math.sin(rotation),
        };
      } else if (position == "bottomRight") {
        let theta = Math.atan2(shape.Size.height, shape.Size.width);
        let rotation = theta - (shape.Orientation * Math.PI) / 180;
        return {
          x: center_x + r * Math.cos(rotation),
          y: center_y - r * Math.sin(rotation),
        };
      } else if (position == "topRight") {
        let theta = Math.atan2(-shape.Size.height, shape.Size.width);
        let rotation = theta - (shape.Orientation * Math.PI) / 180;
        return {
          x: center_x + r * Math.cos(rotation),
          y: center_y - r * Math.sin(rotation),
        };
      } else if (position == "topLeft") {
        let theta = Math.atan2(-shape.Size.height, -shape.Size.width);
        let rotation = theta - (shape.Orientation * Math.PI) / 180;
        return {
          x: center_x + r * Math.cos(rotation),
          y: center_y - r * Math.sin(rotation),
        };
      }
      return null;
    },
    mapCoordinateFromScreenCoordinate(position) {
      return {
        x: position.x * this.currentmap.data.MapInfo.MeterForPixel, //this.map.data.MapInfo.MeterForPixel,
        y: position.y * this.currentmap.data.MapInfo.MeterForPixel, //this.map.data.MapInfo.MeterForPixel,
      };
    },
    screenCoordinateFromMapCoordinate(position) {
      return {
        x: position.x / this.currentmap.data.MapInfo.MeterForPixel, //this.map.data.MapInfo.MeterForPixel,
        y: position.y / this.currentmap.data.MapInfo.MeterForPixel, //this.map.data.MapInfo.MeterForPixel,
      };
    },
    drawShape(ctx, shape, with_alpha = true) {
      let strokeSize = 4;
      if (shape.size != null && shape.size > 0) strokeSize = shape.size;
      ctx.strokeWeight(strokeSize);
      if (shape.shape_type !== "polygon" || shape.Position.length > 1) {
        if (
          shape.shape_type === "line" ||
          (shape.shape_type === "polygon" && shape.isDrawing === true)
        ) {
          let fillColor = ctx.color(shape.color);
          if (with_alpha) fillColor.setAlpha(100);
          ctx.stroke(fillColor);
          let prevPostion = null;
          for (let aPosition of shape.Position) {
            if (prevPostion != null) {
              let screenPosition =
                this.screenCoordinateFromMapCoordinate(aPosition);
              let screenPositionPrev =
                this.screenCoordinateFromMapCoordinate(prevPostion);
              ctx.line(
                screenPosition.x,
                screenPosition.y,
                screenPositionPrev.x,
                screenPositionPrev.y
              );
            }
            prevPostion = aPosition;
          }
        } else if (
          shape.shape_type === "polygon" &&
          shape.isDrawing == undefined
        ) {
          let prevPostion = null;
          let fillColor = ctx.color(shape.color);
          if (with_alpha) fillColor.setAlpha(100);
          ctx.fill(fillColor);
          ctx.noStroke();
          ctx.beginShape();
          let firstPosition = null;
          for (let aPosition of shape.Position) {
            if (firstPosition == null) firstPosition = aPosition;
            let screenPosition =
              this.screenCoordinateFromMapCoordinate(aPosition);
            ctx.vertex(screenPosition.x, screenPosition.y);
          }
          let screenPosition =
            this.screenCoordinateFromMapCoordinate(firstPosition);
          ctx.vertex(screenPosition.x, screenPosition.y);

          ctx.endShape(ctx.CLOSE);
        } else if (shape.shape_type === "rectangle") {
          let strokeColor = ctx.color(this.drawColor);
          strokeColor.setAlpha(0);
          ctx.stroke(strokeColor);
          ctx.strokeWeight(1.5);
          let fillColor = ctx.color(shape.color);
          if (with_alpha) fillColor.setAlpha(100);
          ctx.fill(fillColor);
          let screenPosition = this.screenCoordinateFromMapCoordinate(
            shape.Position
          );
          let centerX = parseInt(
            screenPosition.x + shape.Size.width / this.meterForPixel / 2
          );
          let centerY = parseInt(
            screenPosition.y + shape.Size.height / this.meterForPixel / 2
          );

          if (
            shape.Orientation != null &&
            !isNaN(shape.Orientation) &&
            parseInt(shape.Orientation) !== 0
          ) {
            ctx.push();
            ctx.translate(centerX, centerY);
            ctx.rotate(shape.Orientation);
            ctx.rect(
              0,
              0,
              shape.Size.width / this.meterForPixel,
              shape.Size.height / this.meterForPixel
            );
            ctx.pop();
          } else {
            ctx.rect(
              parseInt(centerX),
              parseInt(centerY),
              parseInt(shape.Size.width / this.meterForPixel),
              parseInt(shape.Size.height / this.meterForPixel)
            );
          }
        } else if (shape.shape_type == "ellipse") {
          ctx.stroke(shape.color);
          let fillColor = ctx.color(shape.color);
          if (with_alpha) fillColor.setAlpha(100);
          ctx.fill(fillColor);
          ctx.noStroke();
          let screenPosition = this.screenCoordinateFromMapCoordinate(
            shape.Position
          );
          let centerX =
            screenPosition.x + shape.Size.width / this.meterForPixel / 2;
          let centerY =
            screenPosition.y + shape.Size.height / this.meterForPixel / 2;
          if (
            shape.Orientation != null &&
            !isNaN(shape.Orientation) &&
            parseInt(shape.Orientation) != 0
          ) {
            ctx.push();
            ctx.translate(centerX, centerY);
            ctx.rotate(-shape.Orientation);
            ctx.ellipse(
              0,
              0,
              shape.Size.width / this.meterForPixel,
              shape.Size.height / this.meterForPixel
            );
            ctx.pop();
          } else {
            ctx.ellipse(
              centerX,
              centerY,
              shape.Size.width / this.meterForPixel,
              shape.Size.height / this.meterForPixel
            );
          }
        }
        if (!this.saveBool) this.drawSelectedShape(shape);
      }
    },
    drawSelectedShape(shape) {
      let index = this.arrSelectedObjects.indexOf(shape);
      if (index == -1) return;
      let x1, y1, x2, y2, width, height;
      if (shape.BoundingBox != null) {
        x1 = shape.BoundingBox.x;
        y1 = shape.BoundingBox.y;
        x2 = shape.BoundingBox.x + shape.BoundingBox.width - 1;
        y2 = shape.BoundingBox.y + shape.BoundingBox.height - 1;
        let screenPosition1 = this.screenCoordinateFromMapCoordinate({
          x: x1,
          y: y1,
        });
        let screenPosition2 = this.screenCoordinateFromMapCoordinate({
          x: x2,
          y: y2,
        });
        width = shape.BoundingBox.width / this.meterForPixel;
        height = shape.BoundingBox.height / this.meterForPixel;
        this.p5.strokeWeight(2);
        this.p5.stroke(0, 0, 255, 255 * 0.5);
        this.p5.noFill();
        this.p5.rect(
          (screenPosition1.x + screenPosition2.x) / 2,
          (screenPosition1.y + screenPosition2.y) / 2,
          width,
          height
        );
      } else {
        let topLeft = this.getShapeBoundingPoint(
          shape,
          "topLeft",
          "drawSelected"
        );
        topLeft = this.screenCoordinateFromMapCoordinate(topLeft);

        let topRight = this.getShapeBoundingPoint(
          shape,
          "topRight",
          "drawSelected"
        );
        topRight = this.screenCoordinateFromMapCoordinate(topRight);

        let bottomRight = this.getShapeBoundingPoint(
          shape,
          "bottomRight",
          "drawSelected"
        );
        bottomRight = this.screenCoordinateFromMapCoordinate(bottomRight);

        let bottomLeft = this.getShapeBoundingPoint(
          shape,
          "bottomLeft",
          "drawSelected"
        );
        bottomLeft = this.screenCoordinateFromMapCoordinate(bottomLeft);
        this.p5.strokeWeight(2);
        this.p5.stroke(0, 0, 255, 255 * 0.5);
        this.p5.noFill();
        this.p5.line(topLeft.x, topLeft.y, topRight.x, topRight.y);
        this.p5.line(topRight.x, topRight.y, bottomRight.x, bottomRight.y);
        this.p5.line(bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y);
        this.p5.line(bottomLeft.x, bottomLeft.y, topLeft.x, topLeft.y);

        this.p5.strokeWeight(2);
        this.p5.stroke(this.p5.color(88, 88, 0));
        this.p5.fill(this.p5.color(255, 0, 0, 255));
        this.p5.ellipse(topLeft.x, topLeft.y, 10 / this.zoom);
        this.p5.ellipse(topRight.x, topRight.y, 10 / this.zoom);
        this.p5.ellipse(bottomRight.x, bottomRight.y, 10 / this.zoom);
        this.p5.ellipse(bottomLeft.x, bottomLeft.y, 10 / this.zoom);
      }
    },
    afterLoadMapImage(doneCallback) {
      if (this.currentmap.data.Node == null) this.currentmap.data.Node = [];
      if (this.currentmap.poi.customPointData == null)
        this.currentmap.poi.customPointData = [];
      if (this.currentmap.data.Shape == null) this.currentmap.data.Shape = [];
      if (this.currentmap.data.MapInfo == null)
        this.currentmap.data.MapInfo = {};
      if (this.currentmap.resolution)
        this.currentmap.data.MapInfo.MeterForPixel = this.currentmap.resolution;
      if (this.currentmap.data.MapInfo.MeterForPixel == null)
        this.currentmap.data.MapInfo.MeterForPixel = 0.1;
      if (this.currentmap.data.MapInfo.GridUnit == null)
        this.currentmap.data.MapInfo.GridUnit = 10;
      if (this.currentmap.data.MapInfo.Author == null)
        this.currentmap.data.MapInfo.Author = "noname";
      if (this.currentmap.data.MapInfo.ZPosition == null)
        this.currentmap.data.MapInfo.ZPosition = 0;

      if (
        this.currentmap.data.MapInfo.ZPosition == 0 &&
        this.currentmap.data.Node.length > 0
      ) {
        this.currentmap.data.MapInfo.ZPosition =
          this.currentmap.data.Node[0].Position.z;
      }
      console.log("# of nodes", this.currentmap.data.Node.length);
      console.log("# of Shape", this.currentmap.data.Shape.length);
      // console.log("Map Data", JSON.stringify(this.currentmap.data, null, 4));
      this.meterForPixel = this.currentmap.data.MapInfo.MeterForPixel;

      this.viewPort = {
        x: 0,
        y: 0,
        width: 0,
        height: 0,
      };

      this.centerMapFocusing(); //map focusing
      this.recalcViewPort();
      this.createNodeMap(this.arrNode);
      this.createEdgeMap(this.arrNode);

      // this.createEdgeGroupMap(this.arrNode);

      doneCallback(null);
    },
    setZoom(newZoom, flag = false, zoomBtn = false, val) {
      if (zoomBtn || !zoomBtn) {
        this.zoomFromMouse = false;
        if (flag || newZoom === 1) {
          this.centerMapFocusing(); //map focusing
        }

        let x, y;
        if (flag || newZoom === 1) {
          x = (this.viewPort.x * newZoom) / newZoom;
          y = (this.viewPort.y * newZoom) / newZoom;
        } else {
          x = (this.viewPort.x * newZoom) / this.zoom;
          y = (this.viewPort.y * newZoom) / this.zoom;
        }

        if (this.store.elevatorMode) {
          //For elevatorPoi
          this.movingRotationBtn();
        }

        this.zoom = newZoom;
        this.moveViewPort(x, y);
        this.recalcViewPort();
      } else {
        this.zoomFromMouse = true;
        let x, y;

        if (val === "zoomIn") {
          this.sf = newZoom;
        } else if (val === "zoomOut") {
          this.sf = newZoom;
        }

        this.zoom = newZoom;
        this.moveViewPort(x, y);
        this.recalcViewPort();
      }
    },
    drawPaint(color) {
      if (this.clickedInsideSelection) {
        const ctx = this.imageContext;
        const _imgData = ctx.getImageData(
          this.selection.x,
          ctx.canvas.height - this.selection.y,
          this.selection.w,
          this.selection.h * -1
        );
        const floodFill = new FloodFill(_imgData);
        floodFill.fill(
          color,
          parseInt(this.paintPos.x - this.selection.x),
          parseInt(this.paintPos.y - (ctx.canvas.height - this.selection.y)),
          0
        );
        ctx.putImageData(
          floodFill.imageData,
          this.selection.x,
          ctx.canvas.height - this.selection.y
        );

        const imgData = ctx.getImageData(
          0,
          0,
          ctx.canvas.width,
          ctx.canvas.height
        );
        let image = this.getImageAfterDrawing(ctx, imgData);
        this.undoStack.push(image);
      } else {
        const context = this.mapImage.canvas.getContext("2d");
        console.log("-------------->>>>>>>", this.mapImage);
        const imgData = context.getImageData(
          0,
          0,
          context.canvas.width,
          context.canvas.height
        );
        const floodFill = new FloodFill(imgData);

        // modify image data
        console.log(this.paintPos.x, this.paintPos.y);
        floodFill.fill(color, this.paintPos.x, this.paintPos.y, 0);
        context.putImageData(floodFill.imageData, 0, 0);
        let image = this.getImageAfterDrawing(context, imgData);
        // console.log(image);
        this.undoStack.push(image);
      }
    },
    hexToRGB(hex) {
      return hex
        .replace(
          /^#?([a-f\d])([a-f\d])([a-f\d])$/i,
          (m, r, g, b) => "#" + r + r + g + g + b + b
        )
        .substring(1)
        .match(/.{2}/g)
        .map((x) => parseInt(x, 16));
    },
    windowResized(p5) {
      let width = this.container.offsetWidth;
      let height = this.container.offsetHeight;
      width = width - 30; //ruler left value subtracted
      height = height - 28; // footer height value subtracted
      this.p5.resizeCanvas(width, height);
      this.centerMapFocusing(); //map focusing when resize the canvas
      this.recalcViewPort();
    },

    // Line drawing

    lineDrawing(xPos, yPos, size) {
      for (let i = xPos - size / 2; i < xPos + size / 2; i++) {
        for (let j = yPos - size / 2; j < yPos + size / 2; j++) {
          this.mapImage.set(i + 0.5, j, this.p5.color(this.drawColor));
        }
      }
    },
    lineEquationFromPoints(p, q) {
      let m = (q.y - p.y) / (q.x - p.x);
      let c = p.y - m * p.x;

      return {
        m: m,
        c: c,
      };
    },
    drawSingleLineSequence(ctx, arrLine) {
      // on first click

      let prevPosition = null;
      for (let currentPosition of arrLine) {
        if (prevPosition == null) {
          this.lineDrawing(currentPosition.x, currentPosition.y);
        } else {
          let coefficients = this.lineEquationFromPoints(
            prevPosition,
            currentPosition
          );
          let i, j;
          if (
            Math.abs(currentPosition.x - prevPosition.x) >=
            Math.abs(currentPosition.y - prevPosition.y)
          ) {
            this.mapImage.loadPixels();
            let size = this.pencilSize;

            let pre =
              prevPosition.x < currentPosition.x
                ? prevPosition
                : currentPosition;
            let cur =
              prevPosition.x < currentPosition.x
                ? currentPosition
                : prevPosition;
            for (
              i = pre.x;
              i <= cur.x;
              i = i + Math.max(Math.round(currentPosition.size / 4), 1)
            ) {
              j = Math.round(coefficients.m * i + coefficients.c);
              this.lineDrawing(i, j, size);
            }
            this.mapImage.updatePixels();
          } else {
            this.mapImage.loadPixels();
            let size = this.pencilSize;
            let pre =
              prevPosition.y < currentPosition.y
                ? prevPosition
                : currentPosition;
            let cur =
              prevPosition.y < currentPosition.y
                ? currentPosition
                : prevPosition;
            i = currentPosition.x;
            for (
              j = pre.y;
              j <= cur.y;
              j = j + Math.max(Math.round(currentPosition.size / 4), 1)
            ) {
              if (prevPosition.x !== currentPosition.x) {
                i = Math.round((j - coefficients.c) / coefficients.m);
              }
              console.log("i and j val are: ", i, j);
              this.lineDrawing(i, j, size);
            }
            this.mapImage.updatePixels();
          }
        }
        prevPosition = currentPosition;
      }
      const imgData = ctx.getImageData(
        0,
        0,
        ctx.canvas.width,
        ctx.canvas.height
      );
      let image = this.getImageAfterDrawing(ctx, imgData);
      // console.log(image);
      this.undoStack.push(image);
    },

    isOutOfCanvas(mouseX, mouseY) {
      if (
        mouseX == null ||
        mouseY == null ||
        mouseX < 0 ||
        mouseY < 0 ||
        this.p5 == null
      )
        return true;
      return (
        mouseX > this.p5.width - SCROLL_BAR_SIZE ||
        mouseY > this.p5.height - SCROLL_BAR_SIZE
      );
    },
    drawMapImage(p5) {
      p5.background(150);
      if (this.mapImage != null) {
        let ctx = this.mapImage.drawingContext;
        ctx.imageSmoothingEnabled = false;
        this.imageContext = ctx;

        ctx = p5.canvas.getContext("2d");
        ctx.imageSmoothingEnabled = false;
        this.htmlContext = ctx;

        p5.push();
        p5.scale(1, -1);

        p5.translate(this.mapImage.width / 2, -this.mapImage.height / 2);
        if (!this.saveBool) {
          p5.image(
            this.mapImage,
            0,
            0,
            this.viewPort.width / this.zoom,
            this.viewPort.height / this.zoom
          );
        } else {
          if (!this.savingShape) {
            p5.image(this.mapImage, 0, 0);
          } else {
          }
        }

        p5.pop();

        if (this.showSelection && this.isSelecting && !this.clearSelection) {
          ctx.save(); // Save current state
          ctx.strokeStyle = "red";
          ctx.lineWidth = 1;
          ctx.setLineDash([4]);
          ctx.strokeRect(
            this.selection.x,
            this.selection.y,
            this.selection.w,
            this.selection.h
          );
          if (this.topLeftHandle && this.isDragging) {
            ctx.lineWidth = 1;
            ctx.strokeRect(this.selection.x, this.selection.y, 20, -20);
          }
          if (this.bottomLeftHandle && this.isDragging) {
            ctx.lineWidth = 1;
            ctx.strokeRect(
              this.selection.x,
              this.selection.y + this.selection.h + 20,
              20,
              -20
            );
          }
          if (this.topRightHandle && this.isDragging) {
            ctx.lineWidth = 1;
            ctx.strokeRect(
              this.selection.x + this.selection.w - 20,
              this.selection.y,
              20,
              -20
            );
          }
          if (this.bottomRightHandle && this.isDragging) {
            ctx.lineWidth = 1;
            ctx.strokeRect(
              this.selection.x + this.selection.w - 20,
              this.selection.y + this.selection.h + 20,
              20,
              -20
            );
          }
          ctx.restore(); // Restore original state
        }
        if (this.countSize == 0) {
          const imgData = this.imageContext.getImageData(
            0,
            0,
            this.imageContext.canvas.width,
            this.imageContext.canvas.height
          );
          this.originalImageValue = this.getImageAfterDrawing(
            this.imageContext,
            imgData
          );
          this.countSize++;
        }
      }
    },
    recalcViewPort() {
      if (this.viewPort == null || this.p5 == null || this.mapImage == null)
        return;

      console.log(
        ">>>> recalcViewPort before",
        this.viewPort.x,
        this.viewPort.y
      );

      this.viewPort.width = this.zoom * this.loadImageWidth;
      this.viewPort.height = this.zoom * this.loadImageHeight;

      if (this.viewPort.width < this.p5.width) {
        this.viewPort.x = -(this.p5.width - this.viewPort.width) / 2;
      }

      if (this.viewPort.height < this.p5.height) {
        this.viewPort.y = -(this.p5.height - this.viewPort.height) / 2;
      }
      console.log(
        ">>>> recalcViewPort after",
        this.viewPort.x,
        this.viewPort.y
      );

      //window size re update
      this.container = this.$refs.p5Container;
      let container = this.container;
      let width = container.offsetWidth;
      let height = container.offsetHeight;
      width = width - 30; //ruler left value subtracted
      //height = height - ; // footer height value subtracted
      this.p5.resizeCanvas(width, height);
      this.recalcRuler();
      this.recalcScrollbar();
    },

    prepareDash(p5) {
      p5.drawingContext.setLineDash([2, 6]);
    },
    finishDash(p5) {
      p5.drawingContext.setLineDash([]);
    },
    isPointInsideRectangle(point, rectangle) {
      const { x: px, y: py } = point;
      // [x1, y1, x2, y2, x3, y3, x4, y4] = rectangle;
      const { x: x1, y: y1 } = rectangle[0];
      const { x: x2, y: y2 } = rectangle[1];
      const { x: x3, y: y3 } = rectangle[2];
      const { x: x4, y: y4 } = rectangle[3];
      // Check if the point is inside the rectangle using the winding number algorithm
      function isInsideWindingNumberAlgorithm(x, y) {
        let windingNumber = 0;

        function isLeft(p1, p2, p3) {
          return (
            (p2[0] - p1[0]) * (p3[1] - p1[1]) -
            (p3[0] - p1[0]) * (p2[1] - p1[1])
          );
        }

        const rectangleVertices = [
          [x1, y1],
          [x2, y2],
          [x3, y3],
          [x4, y4],
        ];

        for (let i = 0; i < 4; i++) {
          const currentVertex = rectangleVertices[i];
          const nextVertex = rectangleVertices[(i + 1) % 4];

          if (currentVertex[1] <= y) {
            if (
              nextVertex[1] > y &&
              isLeft(currentVertex, nextVertex, [x, y]) > 0
            ) {
              windingNumber++;
            }
          } else if (
            nextVertex[1] <= y &&
            isLeft(currentVertex, nextVertex, [x, y]) < 0
          ) {
            windingNumber--;
          }
        }

        return windingNumber !== 0;
      }

      return isInsideWindingNumberAlgorithm(px, py);
    },

    // // Example usage
    // const pointToCheck = [3, 4];
    // const rectangleCoordinates = [1, 2, 5, 2, 5, 6, 1, 6]; // Adjust coordinates as needed

    // const isInside = isPointInsideRectangle(pointToCheck, rectangleCoordinates);
    // console.log(isInside);  // This will output true or false

    pointInsideRect(x, y, rectX, rectY, rectWidth, rectHeight) {
      // console.log('x:', x, 'y:', y, 'rectX', rectX, 'rectY', rectY, 'rectX2', rectX + rectWidth, 'rectY2', rectY + rectHeight)
      let isInside =
        rectX <= x &&
        x <= rectX + rectWidth &&
        rectY <= y &&
        y <= rectY + rectHeight;
      return isInside;
    },
    getDistance(x1, y1, x2, y2) {
      let x_diff = x1 - x2;
      let y_diff = y1 - y2;

      return parseInt(Math.sqrt(x_diff * x_diff + y_diff * y_diff));
    },
    loadMapImage(doneCallback) {
      this.$nextTick(() => {
        this.p5.loadImage(
          `${
            process.env.NODE_ENV === "development"
              ? devmodeImageUrl()
              : getUrl()
          }images/${this.store.sessionId}/${encodeURIComponent(
            this.store.mapFileName
          )}_slam.png`, //here image name dynamically
          (img) => {
            console.log("img", img);
            this.mapImage = img;
            this.currentmap = this.currentMap;
            this.$emit("setcurrentMapImage", img);
            this.$emit("setcurrentMapChanged", false);
            this.loadImageWidth = img.width;
            this.loadImageHeight = img.height;
            //this.p5.image(img, 0, 0);
            this.nodeNameHeader = this.store.floorIndex;
            // Loading POI Details start---------------------------------------
            axios
              .get(
                `${
                  process.env.NODE_ENV === "development"
                    ? devmodeImageUrl()
                    : getUrl()
                }images/${this.store.sessionId}/${encodeURIComponent(
                  this.store.poiFileName
                )}.poi`
              )
              .then((res) => {
                console.log("READING poi FILE -->>", res);
                let textPoi = res.data;
                if (textPoi == null) {
                  console.log("poi file empty");
                } else {
                  let jsonpoi = textPoi;

                  let values = Object.values(jsonpoi.customPointData);
                  for (let val of values) {
                    val.Position = val.pos;
                    delete val.pos;

                    if (typeof val.attributes === "undefined")
                      val.attributes = {};

                    if (typeof val.attributes.callbell === "undefined")
                      val.attributes.callbell = "";
                    if (typeof val.attributes.desc === "undefined")
                      val.attributes.desc = "";
                    if (typeof val.attributes.tel === "undefined")
                      val.attributes.tel = "";
                    if (typeof val.attributes.range === "undefined")
                      if (this.store.showPOILineUp) val.attributes.range = 0.0;
                    if (
                      typeof val.attributes.buildingTransferPoint ===
                      "undefined"
                    )
                      val.attributes.buildingTransferPoint = {};
                    if (
                      typeof val.attributes.buildingTransferPoint
                        .destinationMap === "undefined"
                    )
                      val.attributes.buildingTransferPoint.destinationMap = "";
                    if (
                      typeof val.attributes.buildingTransferPoint.x ===
                      "undefined"
                    )
                      val.attributes.buildingTransferPoint.x = 0;
                    if (
                      typeof val.attributes.buildingTransferPoint.y ===
                      "undefined"
                    )
                      val.attributes.buildingTransferPoint.y = 0;
                    if (
                      typeof val.attributes.buildingTransferPoint.z ===
                      "undefined"
                    )
                      val.attributes.buildingTransferPoint.z = 0;
                    if (
                      typeof val.attributes.buildingTransferPoint.theta ===
                      "undefined"
                    )
                      val.attributes.buildingTransferPoint.theta = 0;

                    if (typeof val.attributes.elevatorID === "undefined")
                      val.attributes.elevatorID = "";
                    if (typeof val.attributes.elevatorVendor === "undefined")
                      val.attributes.elevatorVendor = "";
                    if (typeof val.attributes.elevatorFloor === "undefined")
                      val.attributes.elevatorFloor = "";
                    if (typeof val.attributes.elevatorFloorList === "undefined")
                      val.attributes.elevatorFloorList = "";
                    if (typeof val.attributes.elevatorDoor === "undefined")
                      val.attributes.elevatorDoor = "";
                    if (typeof val.attributes.elevatorInOut === "undefined")
                      val.attributes.elevatorInOut = "";

                    if (typeof val.attributes.macAddress === "undefined")
                      val.attributes.macAddress = "";
                    if (typeof val.attributes.companyName === "undefined")
                      val.attributes.companyName = "";
                  }

                  this.currentmap.poi.customPointData = values;
                  this.currentmap.poi.floorName = jsonpoi.floorName;
                  this.currentmap.poi.floorIndex = jsonpoi.floorIndex;
                  this.currentmap.poi.floorCode = jsonpoi.floorCode;
                  this.currentmap.poi.buildingIndex = jsonpoi.buildingIndex;
                  this.currentmap.poi.z = jsonpoi.z;
                }
              })
              .catch((err) => {
                console.log("POi FILE READING ERROR ->>", err);
                this.currentmap.poi.customPointData = [];
                this.currentmap.poi.floorName = "";
                this.currentmap.poi.floorIndex = this.store.floorIndex;
                this.currentmap.poi.floorCode = this.store.floorIndex;
                this.currentmap.poi.buildingIndex = this.store.buildingIndex;
                this.currentmap.poi.z = "";
              });

            // Loading POI Details done---------------------------------------

            // Reading status file if present
            axios
              .get(
                `${
                  process.env.NODE_ENV === "development"
                    ? devmodeImageUrl()
                    : getUrl()
                }images/${this.store.sessionId}/${encodeURIComponent(
                  this.store.poiFileName
                )}.status`
              )
              .then((res) => {
                console.log("res status file >>>", res.data);
                this.store.mapStatus = res.data;
              })
              .catch((e) => {
                this.store.mapStatus = null;
                console.log("err status file >> ", e);
              });

            // Loading Node and Shape Details start---------------------------------------

            axios
              .get(
                `${
                  process.env.NODE_ENV === "development"
                    ? devmodeImageUrl()
                    : getUrl()
                }images/${this.store.sessionId}/${
                  encodeURIComponent(this.store.mapFileName).split(".")[0]
                }.txt`
              )
              .then((res) => {
                console.log("READING .TXT FILE -->>", res);
                let textData = res.data;
                console.log(
                  "check for data file missing node attribute",
                  textData
                );
                if (!textData.Node) textData.Node = [];
                if (!textData.Shape) textData.Shape = [];
                console.log(
                  "check for setting blank data in node,shape attribute if not present",
                  textData
                );
                if (textData == null) {
                  console.log("data file empty");
                  this.afterLoadMapImage(doneCallback);
                  return;
                }

                this.currentmap.data = textData;

                this.store.slamTypeStatus =
                  this.currentmap.data.MapInfo.hasOwnProperty("SlamType");
                if (this.store.slamTypeStatus)
                  this.store.slamTypeValue =
                    this.currentmap.data.MapInfo.SlamType;
                this.store.nodeIndex = this.currentmap.data.Node.length;
                this.store.shapeIndex = this.currentmap.data.Shape.length;
                this.afterLoadMapImage(doneCallback);
              })
              .catch(() => {
                this.store.mapLoadError = true;
              });

            // Loading Node and Shape Details end---------------------------------------

            console.log("map image is ", this.mapImage);
            this.store.changeMapState(this.mapImage);
            this.$emit("mapImage", this.mapImage);

            this.recalcViewPort();
            this.afterLoadMapImage(doneCallback);
          }
        );
      });
    },

    drawScrollBar(p5) {
      p5.rectMode(p5.CORNER);
      let fillColor = p5.color("#8B8B8B");
      p5.fill(fillColor);
      p5.noStroke();
      // p5.rect(0, p5.height - SCROLL_BAR_SIZE, p5.width, SCROLL_BAR_SIZE);
      // p5.rect(p5.width - SCROLL_BAR_SIZE, 0, SCROLL_BAR_SIZE, p5.height);
      //p5.rect(0 - 60, p5.height - SCROLL_BAR_SIZE - 30, p5.width, SCROLL_BAR_SIZE);
      //p5.rect(p5.width - SCROLL_BAR_SIZE-5, 0-5, SCROLL_BAR_SIZE, p5.height);

      p5.fill(200, 200, 200);
      p5.rect(
        this.scrollBar.horizontal.offset + 42,
        p5.height - SCROLL_BAR_SIZE - 55,
        this.scrollBar.horizontal.size - 85 < 0
          ? 0
          : this.scrollBar.horizontal.size - 85,
        SCROLL_BAR_SIZE,
        50
      );

      if (this.scrollBar.vertical.size > 0) {
        p5.noStroke();
        p5.rect(
          p5.width - SCROLL_BAR_SIZE - 5,
          p5.height -
            this.scrollBar.vertical.offset -
            this.scrollBar.vertical.size -
            SCROLL_BAR_SIZE -
            5,
          SCROLL_BAR_SIZE,
          this.scrollBar.vertical.size,
          50
        );
      }

      p5.fill(50, 50, 50, 100);
      p5.noStroke();

      // p5.rect(
      //   p5.width - SCROLL_BAR_SIZE,
      //   p5.height - SCROLL_BAR_SIZE,
      //   SCROLL_BAR_SIZE,
      //   SCROLL_BAR_SIZE
      // );
      p5.rectMode(p5.CENTER);
    },

    
async draw(p5) {
    if (this.mapImage == null) return;
    p5.push();
    let ctx = this.setupContext(p5);
    this.applyZoom(p5);

    this.drawMapImage(ctx);
    this.drawEdges(ctx);
    this.drawShapes(ctx);
    
    this.drawGuideLine(p5);
    this.drawGrid(p5);
    this.handleSaving(ctx);

    this.drawHandle(p5);
    p5.pop();
    this.drawScrollBar(p5);
},

setupContext(p5) {
    let ctx = p5;
    if (this.saveBool) {
        p5.noSmooth();
        ctx = this.p5.createGraphics(this.mapImage.width, this.mapImage.height);
        ctx.pixelDensity(1);
        ctx.rectMode(ctx.CENTER);
        ctx.imageMode(ctx.CENTER);
        ctx.scale(1, -1);
        ctx.translate(0, -parseInt(ctx.height));
    } else {
        ctx.scale(this.zoom, -this.zoom);
        ctx.translate(
            -this.viewPort.x / this.zoom,
            (-ctx.height - this.viewPort.y + SCROLL_BAR_SIZE) / this.zoom
        );
    }
    return ctx;
},

applyZoom(p5) {
    if (this.zoomFromMouse) {
        p5.translate(this.mx, this.my);
        p5.scale(this.sf);
        p5.translate(-this.mx, -this.my);
    }
},

drawEdges(ctx) {
    if (!this.edgeCache) {
        this.edgeCache = new Map();
    }

    if (!this.edgeCache.has('edges')) {
        this.edgeCache.set('edges', []);
        this.edgeMap.forEach((edgeInfo, edge) => {
            ctx.line(edgeInfo.pos[0].x, edgeInfo.pos[0].y, edgeInfo.pos[1].x, edgeInfo.pos[1].y);
            this.edgeCache.get('edges').push([edgeInfo.pos[0].x, edgeInfo.pos[0].y, edgeInfo.pos[1].x, edgeInfo.pos[1].y]);
        });
    } else {
        this.edgeCache.get('edges').forEach(edge => {
            ctx.line(edge[0], edge[1], edge[2], edge[3]);
        });
    }
},

drawShapes(ctx) {
    if (!this.saveBool) {
        if (this.store.shapeChecked) {
            for (let shape of this.arrShape) {
                this.drawShape(ctx, shape);
            }
        }
        if (this.drawingShape != null) {
            this.drawShape(ctx, this.drawingShape);
        }
    } else {
        let _ctx = this.p5.createGraphics(this.mapImage.width, this.mapImage.height);
        _ctx.pixelDensity(1);
        _ctx.rectMode(_ctx.CENTER);
        _ctx.imageMode(_ctx.CENTER);
        if (!this.mergeImg) {
            _ctx.scale(1, -1);
            _ctx.translate(0, -parseInt(_ctx.height));
        }
        if (this.store.shapeChecked) {
            for (let shape of this.arrShape) {
                this.drawShape(_ctx, shape, false);
            }
        }
    }
},

async handleSaving(ctx) {
    if (this.shapeContext != undefined && this.mergeImg) {
        this.stopSaving = true;
        this.shapeContext = undefined;
    }
    if (this.saveBool) {
        if (!this.savingShape && !this.mergeImg) {
            this.final_ctx = ctx;
            this.saveOutputImage2(ctx);
        } else if (this.savingShape) {
            this.saveShapeImage2(ctx);
        } else if (this.mergeImg) {
            if (!this.stopSaving) {
                this.saveCombinedImage2(this.final_ctx);
            }
            this.mergeImg = false;
            this.saveBool = false;
            this.store.saveMap = false;
            let authToken = await getAuthToken(this.store);
            this.uploadURL = await this.getUploadURL(authToken);
            this.uploadMapImages(this.uploadURL);
            uploadPoiFile(this.store, this.store.saveMapItem.id);
        }
    }
},

      

    getScrollbarAtMouse() {
      if (this.p5 === null) return;
      if (
        this.pointInsideRect(
          this.p5.mouseX,
          this.p5.mouseY,
          this.scrollBar.horizontal.offset + 42,
          this.p5.height - SCROLL_BAR_SIZE - 55,
          this.scrollBar.horizontal.size - 85,
          SCROLL_BAR_SIZE
        )
      ) {
        return this.scrollBar.horizontal;
      }
      if (
        this.pointInsideRect(
          this.p5.mouseX,
          this.p5.height - this.p5.mouseY,
          this.p5.width - SCROLL_BAR_SIZE - 5,
          this.scrollBar.vertical.offset + SCROLL_BAR_SIZE,
          SCROLL_BAR_SIZE,
          this.scrollBar.vertical.size
        )
      ) {
        return this.scrollBar.vertical;
      }
    },

    getScrollRailAtMouse() {
      if (this.p5 == null) return;
      if (
        this.pointInsideRect(
          this.p5.mouseX,
          this.p5.mouseY,
          0,
          this.p5.height - SCROLL_BAR_SIZE,
          this.p5.width - SCROLL_BAR_SIZE,
          SCROLL_BAR_SIZE
        )
      ) {
        if (this.p5.mouseX < this.scrollBar.horizontal.offset)
          return "horizontal_left";
        else return "horizontal_right";
      }
      console.log(
        "point inside rectangle",
        this.p5.mouseX,
        this.p5.mouseY,
        this.p5.width - SCROLL_BAR_SIZE,
        0,
        SCROLL_BAR_SIZE,
        this.p5.height - SCROLL_BAR_SIZE
      );
      if (
        this.pointInsideRect(
          this.p5.mouseX,
          this.p5.mouseY,
          this.p5.width - SCROLL_BAR_SIZE,
          0,
          SCROLL_BAR_SIZE,
          this.p5.height - SCROLL_BAR_SIZE
        )
      ) {
        if (
          this.p5.height - this.p5.mouseY >
          this.scrollBar.vertical.offset + SCROLL_BAR_SIZE
        )
          return "vertical_up";
        else return "vertical_down";
      }
      return null;
    },
    //Redo Undo
    getImageAfterDrawing(ctx, imgData) {
      let MyImage = new Image();
      MyImage.src = getImageURL(imgData, ctx.canvas.width, ctx.canvas.height);
      function getImageURL(_imgData, width, height) {
        let canvas = document.createElement("canvas");
        let newCtx = canvas.getContext("2d");
        canvas.width = width;
        canvas.height = height;
        newCtx.putImageData(_imgData, 0, 0);
        return canvas.toDataURL(); //image URL
      }
      return MyImage;
    },
    resetElevatorPoi() {
      this.store.firstRotate = true;
      this.store.firstButton = true;
      this.store.elevatorMode = false;
      this.store.elevatorPosition.x = null;
      this.store.elevatorPosition.y = null;
      this.store.elevatorButton.x = null;
      this.store.elevatorButton.y = null;
      this.elevatorPoi = [];
      this.angle = 0;
      this.p5.cursor(this.p5.ARROW);
    },
    mousePressed(p5, mouseX, mouseY, pmouseX, pmouseY) {
      console.log("mouse pressed");
      this.store.dragMove = true;
      if (this.store.dragScroll.ctrl) {
        this.store.dragScroll.press = true;
      }

      this.mousePressPosition = {
        x: mouseX,
        y: mouseY,
      };
      this.mx = this.mousePressPosition.x;
      this.my = this.mousePressPosition.y;
      // Edge Settings
      //let mapCoord = this.mapCoordinateFromScreenCoordinate(this.mousePressPosition)
      if (this.subMenuSelection === "edgeSetting" && this.isCtrlPressed) {
        this.selectEdge(this.mousePressPosition);
      }
      //--

      if (this.store.elevatorMode) {
        if (
          this.pointInsideRect(
            mouseX,
            mouseY,
            this.store.elevatorButton.x - 19 / this.zoom,
            this.store.elevatorButton.y - 13 / this.zoom,
            this.elevTextWidth - 1 / this.zoom,
            25 / this.zoom
          )
        ) {
          this.resetElevatorPoi();
          this.$emit("backToMenuSelect");
        }
      }

      console.log("mouse positions", mouseX, mouseY);
      let objectAtMouse = this.getObjectAtMouse(mouseX, mouseY);
      if (!objectAtMouse?.attributes?.gid && !this.store.createPoiLineUp) {
        this.store.setcurrentChildgid("");
        this.store.setcurrentChildOrder(0);
      }
      // Line Drawing
      let currentXPos = Math.floor(mouseX);
      let currentYPos = Math.floor(mouseY);
      let lineSizeElement = this.pencilSize;
      let lineSize =
        lineSizeElement === null ||
        lineSizeElement === 0 ||
        lineSizeElement === ""
          ? 1
          : lineSizeElement;

      // selection bs
      if (
        this.mousePressPosition.x > this.selection.x &&
        this.mousePressPosition.y < this.selection.y &&
        this.mousePressPosition.x < this.selection.x + this.selection.w &&
        this.mousePressPosition.y > this.selection.y + this.selection.h
      ) {
        this.clickedInsideSelection = true;
        console.log("clicked inside selection ?", this.clickedInsideSelection);
        if (
          this.mousePressPosition.x > this.selection.x &&
          this.mousePressPosition.y < this.selection.y &&
          this.mousePressPosition.x < this.selection.x + 20 &&
          this.mousePressPosition.y > this.selection.y - 20
        ) {
          this.topLeftHandle = true;
          this.topRightHandle = false;
          this.bottomLeftHandle = false;
          this.bottomRightHandle = false;
          this.isDragging = true;
        } else if (
          this.mousePressPosition.x < this.selection.x + this.selection.w &&
          this.mousePressPosition.y < this.selection.y &&
          this.mousePressPosition.x >
            this.selection.x + this.selection.w - 20 &&
          this.mousePressPosition.y > this.selection.y - 20
        ) {
          this.topRightHandle = true;
          this.topLeftHandle = false;
          this.bottomLeftHandle = false;
          this.bottomRightHandle = false;
          this.isDragging = true;
        } else if (
          this.mousePressPosition.x > this.selection.x &&
          this.mousePressPosition.y > this.selection.y + this.selection.h &&
          this.mousePressPosition.x < this.selection.x + 20 &&
          this.mousePressPosition.y < this.selection.y + this.selection.h + 20
        ) {
          this.bottomLeftHandle = true;
          this.topLeftHandle = false;
          this.topRightHandle = false;
          this.bottomRightHandle = false;
          this.isDragging = true;
        } else if (
          this.mousePressPosition.x < this.selection.x + this.selection.w &&
          this.mousePressPosition.y > this.selection.y + this.selection.h &&
          this.mousePressPosition.x >
            this.selection.x + this.selection.w - 20 &&
          this.mousePressPosition.y < this.selection.y + this.selection.h + 20
        ) {
          this.bottomRightHandle = true;
          this.bottomLeftHandle = false;
          this.topLeftHandle = false;
          this.topRightHandle = false;
          this.isDragging = true;
        }
      } else {
        this.clickedInsideSelection = false;
        console.log("clicked inside selection ?", this.clickedInsideSelection);
      }

      if (!this.clickedInsideSelection) {
        this.showSelection = false;
      }

      //Move
      if (this.isPasted) {
        this._DrawImage(this.imageContext);
        this.isMoved = true;
        this.isPasted = false;
        const imgData = this.imageContext.getImageData(
          0,
          0,
          this.imageContext.canvas.width,
          this.imageContext.canvas.height
        );
        let image = this.getImageAfterDrawing(this.imageContext, imgData);
        this.undoStack.push(image);
      }
      //COPY PASTE SELECTION
      if (this.subMenuSelection == "menuShapeSelect") {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.isSelecting = true;

        console.log("this.isselecting is set true here");
      } else {
        this.isSelecting = false;
      }
      if (this.isLineDrawing) {
        if (
          p5.mouseIsPressed &&
          p5.keyIsDown(p5.SHIFT) &&
          p5.keyIsDown(p5.ALT)
        ) {
          let theta = p5.radians(15); // Convert 15 degrees to radians
          let x = currentXPos;
          let y = this.imageContext.canvas.height - currentYPos;

          let angle = Math.atan2(y - this.linePoints.y, x - this.linePoints.x);

          // Convert the angle to degrees
          angle = p5.degrees(angle);

          // Round the angle to the nearest 15 degrees
          angle = p5.round(angle / 15) * 15;

          // Convert the angle back to radians
          angle = p5.radians(angle);
          // Calculate the new mouse position
          let length = p5.dist(this.linePoints.x, this.linePoints.y, x, y);
          let newX = this.linePoints.x + length * Math.cos(angle);
          let newY = this.linePoints.y + length * Math.sin(angle);
          this.linePoints.x = newX;
          this.linePoints.y = newY;
          this.linePoints.size = lineSize;
          this.linePoints.color = this.drawColor;
          console.error(
            "mouse is pressed with shift and ALT key down4",
            this.linePoints,
            x,
            y
          );
          this.guideLine = this.tempGuideline;
        } else {
          this.linePoints.x = currentXPos;
          this.linePoints.y = this.imageContext.canvas.height - currentYPos;
          this.linePoints.size = lineSize;
          this.linePoints.color = this.drawColor;
          this.guideLine = {
            x: mouseX,
            y: mouseY,
            x2: mouseX,
            y2: mouseY,
            drawMeter: true,
          };
        }
      }

      //COPY PASTE
      if (!this.clickedInsideSelection && this.isSelecting) {
        this.selection.x = currentXPos;
        this.selection.y = currentYPos;
        this.initialSelection.x = currentXPos;
        this.initialSelection.y = currentYPos;
      }

      if (this.subMenuSelection === "menuRuler") {
        if (this.guideLine == null) {
          this.guideLine = {
            x: mouseX,
            y: mouseY,
            x2: mouseX,
            y2: mouseY,
            rulerFixed: false,
            drawMeter: true,
          };
        } else {
          if (this.guideLine.rulerFixed == false) {
            this.guideLine.x2 = mouseX;
            this.guideLine.y2 = mouseY;
            this.guideLine.rulerFixed = true;
          } else {
            this.guideLine = {
              x: mouseX,
              y: mouseY,
              x2: mouseX,
              y2: mouseY,
              rulerFixed: false,
              drawMeter: true,
            };
          }
        }
      }
      console.log("newPoi", this.subMenuSelection, this.store.createPoiLineUp);
      if (["newNode"].includes(this.subMenuSelection)) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.newNode(mouseX, mouseY);
      } else if (this.subMenuSelection == "newPoi") {
        if (this.store.createPoiLineUp) {
          this.newPoi(mouseX, mouseY, p5);
        } else {
          let result = this.ctrlClickFun();
          if (result === "exit") return;
          this.newPoi(mouseX, mouseY, p5);
        }
      } else if (
        this.subMenuSelection == "AutoCreatePOI" &&
        this.store.autoCreateBool
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.newPoi(mouseX, mouseY, p5);
      } else if (this.subMenuSelection == "menuElevator") {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.newPoi(mouseX, mouseY, p5);
      } else if (this.subMenuSelection == "linkNode") {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.linkNode(mouseX, mouseY);
      } else if (this.subMenuSelection == "unlinkNode") {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.unlinkNode(mouseX, mouseY);
      } else if (
        this.subMenuSelection == "autoNode" &&
        !this.showDialogAutoCreate
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        if (this.guideLine == null) {
          this.guideLine = {
            x: mouseX,
            y: mouseY,
            x2: mouseX,
            y2: mouseY,
            drawMeter: true,
          };
          console.log(this.guideLine);
        } else {
          this.arrAutoNode.forEach((node, index, array) => {
            if (index < array.length - 1) {
              this.createEdgeLink(node, array[index + 1], false);
            }
          });
          this.addObjects(this.arrAutoNode, true);
          this.arrAutoNode = [];
          this.guideLine = null;
          this.$emit("backToMenuSelect");
        }
      } else if (
        this.subMenuSelection == "menuMultiplePoi" &&
        !this.showDialogAutoCreate
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        if (this.guideLine == null) {
          this.guideLine = {
            x: mouseX,
            y: mouseY,
            x2: mouseX,
            y2: mouseY,
            drawMeter: true,
          };
          console.log(this.guideLine);
        } else {
          this.addObjects(this.arrAutoPoi, true);
          for (let [index, poi] of this.arrAutoPoi.entries()) {
            poi.name.en =
              poi.name["en-US"] =
              poi.name.kr =
              poi.name["ko-KR"] =
                this.gettingPoiNameNumber(index === 0);
          }
          this.arrAutoPoi = [];
          this.guideLine = null;
          this.$emit("backToMenuSelect");
        }
      }

      if (["menuPaint"].includes(this.subMenuSelection)) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.paintPos.x = parseInt(mouseX);
        this.paintPos.y = parseInt(this.imageContext.canvas.height - mouseY);
        console.log("paint pos is ", this.paintPos, this.drawColor);
        let color = this.hexToRGB(this.drawColor);
        console.log("palette color ", this.drawColor);
        let colorString = `rgba(${color[0]},${color[1]},${color[2]},1)`;
        if (this.isOutOfCanvas(p5.mouseX, p5.mouseY)) return;
        if (!this.colorPlateModel) this.drawPaint(colorString);
      }

      // Single Pencil click
      if (["menuPencil"].includes(this.subMenuSelection)) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        //this.p5.cursor(this.p5.CROSS);
        //this.p5.noCursor();
        console.log("pencilclicked");
        this.dragflag = true;
        if (this.mapImage != null) {
          this.mapImage.loadPixels();
          let size = this.pencilSize;
          //let y = this.loadImageHeight - mouseY;
          let xVal = Math.round((this.viewPort.x + this.p5.mouseX) / this.zoom);
          let y =
            this.loadImageHeight -
            Math.round(
              (this.viewPort.y -
                SCROLL_BAR_SIZE +
                this.p5.height -
                this.p5.mouseY) /
                this.zoom
            );
          let rectX = Math.round((this.viewPort.x + p5.mouseX) / this.zoom);
          let rectY =
            this.loadImageHeight -
            Math.round(
              (this.viewPort.y - SCROLL_BAR_SIZE + p5.height - p5.mouseY) /
                this.zoom
            );
          let rectWidth = this.pencilSize;
          let rectHeight = this.pencilSize;

          let topLeft = { x: rectX, y: rectY };
          let topRight = { x: rectX + rectWidth, y: rectY };
          let bottomLeft = { x: rectX, y: rectY + rectHeight };
          let bottomRight = { x: rectX + rectWidth, y: rectY + rectHeight };

          let offsetX = this.pencilSize % 2 === 0 ? 0.5 : 0;
          let offsetY = this.pencilSize % 2 === 0 ? 0.5 : 0;

          console.log("Top Left: ", topLeft);
          console.log("Top Right: ", topRight);
          console.log("Bottom Left: ", bottomLeft);
          console.log("Bottom Right: ", bottomRight);

          console.log("pixel drawn from else condition", xVal, y);
          // if(this.pencilSize % 2 === 0) {
          for (let i = -rectWidth / 2; i < rectWidth / 2; i++) {
            for (let j = -rectHeight / 2; j < rectHeight / 2; j++) {
              let pixelX = topLeft.x + i + offsetX;
              let pixelY = topLeft.y + j + offsetY;

              console.log("Pixel coordinate1", pixelX, pixelY);

              // Make sure pixelX and pixelY are within the bounds of your canvas or image

              this.mapImage.set(pixelX, pixelY, this.p5.color(this.drawColor));
              // }
            }
          }

          if (!this.isLineDrawing) this.mapImage.updatePixels();
          return;
        }
      }

      if (
        this.subMenuSelection === "menuDrawLine" ||
        this.subMenuSelection === "menuDrawPolygon"
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        this.guideLine = {
          x: mouseX,
          y: mouseY,
          x2: mouseX,
          y2: mouseY,
          drawMeter: true,
        };
        let shape_type =
          this.subMenuSelection === "menuDrawLine" ? "line" : "polygon";
        if (this.drawingShape == null) {
          let mapPosition = this.mapCoordinateFromScreenCoordinate({
            x: mouseX,
            y: mouseY,
          });
          this.drawingShape = {
            Position: [mapPosition],
            Size: {
              width: 1,
              height: 1,
            },
            isDrawing: true,
            shape_type: shape_type,
            domain: this.currentDomain.name,
            color: this.drawColor.toUpperCase(),
            size: this.pencilSize,
            __index: this.store.shapeIndex,
          };
          this.store.shapeIndex++;
        } else {
          let mapPosition = this.mapCoordinateFromScreenCoordinate({
            x: mouseX,
            y: mouseY,
          });
          if (this.pencilLineDrawing) {
            let position = {};
            position.x = mouseX * this.currentmap.data.MapInfo.MeterForPixel;
            position.y = mouseY * this.currentmap.data.MapInfo.MeterForPixel;
            console.log(position, this.drawingShape.Position);
            this.drawingShape.Position.push(position);
            return;
          }
          this.drawingShape.Position.push(mapPosition);
        }
        p5.keypressed = (event) => {
          parentThis.keypressed(p5, mouseX, mouseY);
        };
        return;
      } else if (this.subMenuSelection === "menuDrawRectangle") {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        let mapPosition = this.mapCoordinateFromScreenCoordinate({
          x: mouseX,
          y: mouseY,
        });
        console.log("mapPosition", mapPosition);
        this.drawingShape = {
          Position: mapPosition,
          Size: {
            width: 1,
            height: 1,
          },
          shape_type: "rectangle",
          domain: this.currentDomain.name,
          color: this.drawColor.toUpperCase(),
          size: this.pencilSize,
          __index: this.store.shapeIndex,
        };
        this.store.shapeIndex++;
        return;
      } else if (this.subMenuSelection == "menuDrawEllipse") {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        let mapPosition = this.mapCoordinateFromScreenCoordinate({
          x: mouseX,
          y: mouseY,
        });
        console.log("mapPosition", mapPosition);
        this.drawingShape = {
          Position: mapPosition,
          Size: {
            width: 1,
            height: 1,
          },
          shape_type: "ellipse",
          domain: this.currentDomain.name,
          color: this.drawColor.toUpperCase(),
          size: this.pencilSize,
          __index: this.store.shapeIndex,
        };
        this.store.shapeIndex++;
      } else if (
        this.subMenuSelection === "menuSelect" &&
        this.menuSelection !== "image"
      ) {
        console.log("mousePressed keyCode", objectAtMouse);

        let handleAtMouse = this.getHandleAtMouse(mouseX, mouseY);
        if (handleAtMouse != null) {
          this.selectedHandle = handleAtMouse;
          return;
        }
        this.selectedHandle = null;
        if (objectAtMouse == null) {
          this.selectObject(objectAtMouse, false);
        } else {
          if (this.arrSelectedObjects.length === 0) {
            this.getCtxData();
            this.polygonBool = this.getShapeDataAtPosForPolygon(
              Math.floor(mouseX),
              this.mapImage.height - Math.floor(mouseY),
              this.shapeContextForValidation
            );
            if (
              !this.polygonBool &&
              ["rectangle", "polygon", "ellipse", "line"].includes(
                objectAtMouse.shape_type
              )
            )
              return;

            this.selectObject(objectAtMouse, false);
            console.log("improve polygon", objectAtMouse);

            if (p5.mouseButton == p5.RIGHT) {
              this.targetContextMenu = true;
            }
          } else {
            this.getCtxData();
            this.polygonBool = this.getShapeDataAtPosForPolygon(
              Math.floor(mouseX),
              this.mapImage.height - Math.floor(mouseY),
              this.shapeContextForValidation
            );
            let isMultiSelectKeyDown =
              p5.keyIsDown(p5.SHIFT) || p5.keyIsDown(p5.CONTROL);
            if (
              !this.polygonBool &&
              ["rectangle", "polygon", "ellipse", "line"].includes(
                objectAtMouse.shape_type
              ) &&
              !isMultiSelectKeyDown
            ) {
              this.selectObject(null, false);
            } else {
              if (
                !this.polygonBool &&
                ["rectangle", "polygon", "ellipse", "line"].includes(
                  objectAtMouse.shape_type
                ) &&
                isMultiSelectKeyDown
              ) {
                this.selectObject(null, isMultiSelectKeyDown);
              } else {
                this.selectObject(objectAtMouse, isMultiSelectKeyDown);
              }
            }
          }
        }
        this.selectedObject = objectAtMouse; // remove this line if not wokring
      }
    },
    movingRotateIcon(p5, mouseX, mouseY, pmouseX, pmouseY) {
      this.p5.cursor(this.p5.MOVE);

      // if (
      //   Math.cos(this.angle * (Math.PI / 180)) >= 0 &&
      //   Math.sin(this.angle * (Math.PI / 180)) >= 0
      // ) {
      //   // First quad
      //   if (
      //     mouseX > this.store.elevatorPosition.x ||
      //     mouseY < this.store.elevatorPosition.y
      //   ) {
      //     this.angle -= 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "clock"
      //     );
      //   } else {
      //     this.angle += 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "anticlock"
      //     );
      //   }
      // } else if (Math.sin(this.angle * (Math.PI / 180)) >= 0) {
      //   // Second quad
      //   if (
      //     mouseX > this.store.elevatorPosition.x ||
      //     mouseY > this.store.elevatorPosition.y
      //   ) {
      //     this.angle -= 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "clock"
      //     );
      //   } else {
      //     this.angle += 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "anticlock"
      //     );
      //   }
      // } else if (Math.tan(this.angle * (Math.PI / 180)) >= 0) {
      //   // Third quad
      //   if (
      //     mouseX < this.store.elevatorPosition.x ||
      //     mouseY > this.store.elevatorPosition.y
      //   ) {
      //     this.angle -= 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "clock"
      //     );
      //   } else {
      //     this.angle += 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "anticlock"
      //     );
      //   }
      // } else if (Math.cos(this.angle * (Math.PI / 180)) >= 0) {
      //   // Fourth quad
      //   if (
      //     mouseX < this.store.elevatorPosition.x ||
      //     mouseY < this.store.elevatorPosition.y
      //   ) {
      //     this.angle -= 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "clock"
      //     );
      //   } else {
      //     this.angle += 1;
      //     this.rotatePoi(
      //       this.arrSelectedObjects,
      //       this.store.elevatorPosition.x,
      //       this.store.elevatorPosition.y,
      //       this.angle,
      //       "anticlock"
      //     );
      //   }
      // }

      this.angle -= 1;
      this.rotatePoi(
        this.arrSelectedObjects,
        this.store.elevatorPosition.x,
        this.store.elevatorPosition.y,
        this.angle,
        "clock"
      );
    },
    movingRotationBtn() {
      this.store.elevatorPositionRotate.x = this.rotatePoiZoom.newElevPos.x;
      this.store.elevatorPositionRotate.y = this.rotatePoiZoom.newElevPos.y;

      this.store.elevatorButton.x = this.rotatePoiZoom.newElevPosBtn.x;
      this.store.elevatorButton.y = this.rotatePoiZoom.newElevPosBtn.y;
    },

    mouseMoved(p5, mouseX, mouseY, pmouseX, pmouseY) {
      this.mouseMovePosition = {
        x: Math.floor(mouseX),
        y: Math.floor(mouseY),
      };

      if (this.store.elevatorMode) {
        if (
          this.pointInsideRect(
            mouseX,
            mouseY,
            this.store.elevatorButton.x - 19 / this.zoom,
            this.store.elevatorButton.y - 13 / this.zoom,
            this.elevTextWidth - 1 / this.zoom,
            25 / this.zoom
          )
        ) {
          this.p5.cursor(this.p5.HAND);
        } else {
          this.p5.cursor(this.p5.MOVE);
        }
      }

      if (!this.store.elevatorMode && this.subMenuSelection !== "menuPencil") {
        this.p5.cursor(this.p5.ARROW);
      }

      if (this.menuSelection == "image") {
        document.addEventListener(
          "mousemove",
          this.$emit(
            "UndoAndRedoImageBuffer",
            this.undoStack.length,
            this.redoStack.length
          ),
          false
        );

        var imageBufferData = this;

        document.getElementById("UndoImageEditor").onclick = () => {
          imageBufferData.undoImageBuffer();
          imageBufferData.$emit(
            "UndoAndRedoImageBuffer",
            imageBufferData.undoStack.length,
            imageBufferData.redoStack.length
          );
        };

        document.getElementById("RedoImageEditor").onclick = () => {
          imageBufferData.redoImageBuffer();
          imageBufferData.$emit(
            "UndoAndRedoImageBuffer",
            imageBufferData.undoStack.length,
            imageBufferData.redoStack.length
          );
        };
      }
      this.currentX = mouseX;
      this.currentY = mouseY;
      if (
        mouseX > this.selection.x &&
        mouseY < this.selection.y &&
        mouseX < this.selection.x + 20 &&
        mouseY > this.selection.y - 20
      ) {
        this.topLeftHandle = true;
        this.topRightHandle = false;
        this.bottomLeftHandle = false;
        this.bottomRightHandle = false;
        this.isDragging = true;
      } else if (
        mouseX < this.selection.x + this.selection.w &&
        mouseY < this.selection.y &&
        mouseX > this.selection.x + this.selection.w - 20 &&
        mouseY > this.selection.y - 20
      ) {
        this.topRightHandle = true;
        this.topLeftHandle = false;
        this.bottomLeftHandle = false;
        this.bottomRightHandle = false;
        this.isDragging = true;
      } else if (
        mouseX > this.selection.x &&
        mouseY > this.selection.y + this.selection.h &&
        mouseX < this.selection.x + 20 &&
        mouseY < this.selection.y + this.selection.h + 20
      ) {
        this.bottomLeftHandle = true;
        this.topLeftHandle = false;
        this.topRightHandle = false;
        this.bottomRightHandle = false;
        this.isDragging = true;
      } else if (
        mouseX < this.selection.x + this.selection.w &&
        mouseY > this.selection.y + this.selection.h &&
        mouseX > this.selection.x + this.selection.w - 20 &&
        mouseY < this.selection.y + this.selection.h + 20
      ) {
        this.bottomRightHandle = true;
        this.bottomLeftHandle = false;
        this.topLeftHandle = false;
        this.topRightHandle = false;
        this.isDragging = true;
      }

      if (this.isPasted) {
        this.selection.x = mouseX;
        this.selection.y = mouseY;

        this._Go(this.imageContext);
      }
      if (
        this.subMenuSelection == "menuDrawLine" ||
        this.subMenuSelection == "menuDrawPolygon"
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        console.log("this.drawingShape", this.drawingShape);
        if (this.guideLine != null) {
          this.guideLine.x2 = mouseX;
          this.guideLine.y2 = mouseY;
        }
      }

      if (this.subMenuSelection === "menuRuler" && this.guideLine != null) {
        if (this.guideLine.rulerFixed == false) {
          this.guideLine.x2 = mouseX;
          this.guideLine.y2 = mouseY;
        }
      }

      this.currentX = mouseX;
      this.currentY = mouseY;

      if (this.subMenuSelection == "autoNode" && this.guideLine != null) {
        this.guideLine.x2 = mouseX;
        this.guideLine.y2 = mouseY;
        this.arrAutoNode = [];
        let distance = this.getDistance(
          this.guideLine.x,
          this.guideLine.y,
          this.guideLine.x2,
          this.guideLine.y2
        );

        let angle = Math.atan2(
          this.guideLine.y2 - this.guideLine.y,
          this.guideLine.x2 - this.guideLine.x
        );
        let step_distance;
        let count;
        if (
          this.store.autoCreateType == "interval" &&
          this.store.IntervalVal > 0
        ) {
          count =
            (distance * this.currentmap.data.MapInfo.MeterForPixel) /
            this.store.IntervalVal;
          step_distance = distance / count;
        } else if (
          this.store.autoCreateType == "count" &&
          this.store.CountVal > 1
        ) {
          count = this.store.CountVal - 1;
          step_distance = distance / count;
        }
        for (let i = 0; i <= count; i++) {
          let x = this.guideLine.x + step_distance * i * Math.cos(angle);
          let y = this.guideLine.y + step_distance * i * Math.sin(angle);
          let lastNewNodeID;
          if (i == 0) lastNewNodeID = this.getLastNodeID();
          else lastNewNodeID = this.arrAutoNode[i - 1].ID;
          let mapPosition = this.mapCoordinateFromScreenCoordinate({
            x: x,
            y: y,
          });
          let newNode = this.createNewNode(
            mapPosition.x,
            mapPosition.y,
            this.mapInfoZPosition,
            lastNewNodeID,
            this.currentmap.data.Node.length + i
          );
          if (this.autoCreatBaseNode != null) {
            newNode.Type = this.autoCreatBaseNode.Type;
            newNode.Orientation = this.autoCreatBaseNode.Orientation;
            newNode.Property = JSON.parse(
              JSON.stringify(this.autoCreatBaseNode.Property)
            );
          }
          this.arrAutoNode.push(newNode);
        }
      }

      if (
        this.subMenuSelection == "menuMultiplePoi" &&
        this.guideLine != null
      ) {
        this.guideLine.x2 = mouseX;
        this.guideLine.y2 = mouseY;
        this.arrAutoPoi = [];
        let distance = this.getDistance(
          this.guideLine.x,
          this.guideLine.y,
          this.guideLine.x2,
          this.guideLine.y2
        );

        let angle = Math.atan2(
          this.guideLine.y2 - this.guideLine.y,
          this.guideLine.x2 - this.guideLine.x
        );
        let step_distance;
        let count;
        if (this.store.poiIntervalValue > 0) {
          count =
            (distance * this.currentmap.data.MapInfo.MeterForPixel) /
            this.store.IntervalVal;
          step_distance = distance / count;
        }
        for (let i = 0; i <= count; i++) {
          let x = this.guideLine.x + step_distance * i * Math.cos(angle);
          let y = this.guideLine.y + step_distance * i * Math.sin(angle);

          let mapPosition = this.mapCoordinateFromScreenCoordinate({
            x: x,
            y: y,
          });
          let newPoi = this.createNewPoi(mapPosition.x, mapPosition.y);

          this.arrAutoPoi.push(newPoi);
        }
      }

      if (this.isLineDrawing) {
        if (this.guideLine != null) {
          this.guideLine.x2 = mouseX;
          this.guideLine.y2 = mouseY;
        }
      }

      if (
        this.subMenuSelection == "menuDrawLine" ||
        this.subMenuSelection == "menuDrawPolygon"
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        console.log("this.drawingShape", this.drawingShape);
        if (this.guideLine != null) {
          this.guideLine.x2 = mouseX;
          this.guideLine.y2 = mouseY;
        }
      }
      // 너무 자주 보내면 좌표값 표시 갱신하는데 부하가 걸려서 마우스가 덕덕거린다.
      if (this.isDialogOpen()) return;
      let now = new Date().getTime();

      if (this.before == null || now - this.before > 100) {
        //???? 임의로 정한 인터벌??

        if (this.store.mapImageLoad) {
          //after mapImage load
          let mapPosition = this.mapCoordinateFromScreenCoordinate({
            x: mouseX,
            y: mouseY,
          });

          this.$emit(
            "p5MouseMoved",
            mouseX,
            mouseY,
            mapPosition.x,
            mapPosition.y
          );
        }
        this.before = now;
      }
    },
    _ResetCanvas(context) {
      context.drawImage(this.previousStoredImages[this.pasteIndex - 1], 0, 0);
    },
    _MouseEvents(ctx) {
      ctx.canvas.onmousedown = function (e) {
        var mouseX = e.pageX - this.offsetLeft;
        var mouseY = e.pageY - this.offsetTop;

        if (
          mouseX >= this.currentX - this.imageSourceData.width / 2 &&
          mouseX <= this.currentX + this.imageSourceData.width / 2 &&
          mouseY >= this.currentY - this.imageSourceData.height / 2 &&
          mouseY <= this.currentY + this.imageSourceData.height / 2
        ) {
          this.isDraggable = true;
        }
      };
      ctx.canvas.onmousemove = function (e) {
        if (this.isDraggable) {
          this.currentX = e.pageX - this.offsetLeft;
          this.currentY = e.pageY - this.offsetTop;
        }
      };
      ctx.canvas.onmouseup = function (e) {
        this.isDraggable = false;
      };
      ctx.canvas.onmouseout = function (e) {
        this.isDraggable = false;
      };
    },
    _DrawImage(context) {
      context.drawImage(
        this.imageSourceData,
        this.currentX,
        this.imageContext.canvas.height - this.currentY
      );
      var position = {
        x: this.currentX,
        y: this.imageContext.canvas.height - this.currentY,
      };
      console.log("Positions:", position);
      this.PastedArrayPositions.push(position);
    },
    //MoveTool
    _Go(ctx) {
      this._MouseEvents(ctx);
      if (!this.isMoved) {
        setInterval(
          this._ResetCanvas(this.imageContext),
          this._DrawImage(this.imageContext),
          1000 / 30
        );
      }
    },
    mouseDragged(p5, mouseX, mouseY, pmouseX, pmouseY) {
      console.log("im dragged");
      // Add a variable to check if the pencil was selected before the drag operation
      let wasPencilSelected = this.currentSubMenuSelection === "menuPencil";
      if (this.getObjectType(mouseX, mouseY) === this.$t("common.node"))
        this.store.updateSelectedEdgeGroup = true;
      if (this.store.objectFlag) return;
      if (this.store.dragScroll.ctrl) {
        this.store.dragScroll.drag = true;
        // If the pencil was selected before the drag operation, reselect it after the operation
        if (wasPencilSelected) {
          this.currentSubMenuSelection = "menuPencil";
        }
      }
      this.drawnLines.push({
        mouseX: p5.mouseX,
        mouseY: p5.mouseY,
        pmouseX: p5.pmouseX,
        pmouseY: p5.pmouseY,
      });

      if (this.store.elevatorMode) {
        // elevator POI
        this.movingRotateIcon(p5, mouseX, mouseY, pmouseX, pmouseY);
        this.movingRotationBtn();
      }

      //copy paste
      if (this.isSelecting && !this.isDragging) {
        this.selection.w = mouseX - this.selection.x;
        this.selection.h = mouseY - this.selection.y;
        this.showSelection = true;
      }

      if (this.isDragging) {
        this.dragTopLeftPos.x = this.selection.x;
        this.dragTopLeftPos.y = this.selection.y;
        if (
          this.topLeftHandle &&
          mouseX < this.dragTopLeftPos.x + this.selection.w &&
          mouseY > this.dragTopLeftPos.y + this.selection.h
        ) {
          this.selection.x = mouseX;
          this.selection.y = mouseY;
          this.selection.w =
            this.selection.w - (mouseX - this.dragTopLeftPos.x);
          this.selection.h =
            (this.selection.h * -1 - (this.dragTopLeftPos.y - mouseY)) * -1;
        } else if (
          this.topRightHandle &&
          mouseX > this.dragTopLeftPos.x &&
          mouseY > this.dragTopLeftPos.y + this.selection.h
        ) {
          this.selection.y = mouseY;
          this.selection.w =
            this.selection.w -
            (this.dragTopLeftPos.x + this.selection.w - mouseX);
          this.selection.h =
            (this.selection.h * -1 - (this.dragTopLeftPos.y - mouseY)) * -1;
        } else if (
          this.bottomLeftHandle &&
          mouseX < this.dragTopLeftPos.x + this.selection.w &&
          mouseY < this.dragTopLeftPos.y
        ) {
          this.selection.x = mouseX;
          this.selection.h =
            (this.selection.h * -1 -
              (mouseY - (this.dragTopLeftPos.y + this.selection.h))) *
            -1;
          this.selection.w =
            this.selection.w - (mouseX - this.dragTopLeftPos.x);
        } else if (
          this.bottomRightHandle &&
          mouseX > this.dragTopLeftPos.x &&
          mouseY < this.dragTopLeftPos.y
        ) {
          this.selection.w =
            this.selection.w -
            (this.dragTopLeftPos.x + this.selection.w - mouseX);
          this.selection.h =
            (this.selection.h * -1 -
              (mouseY - (this.dragTopLeftPos.y + this.selection.h))) *
            -1;
        }
      }
      if (["menuPencil"].includes(this.subMenuSelection)) {
        if (this.store.miniMapFlag) return;
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        //this.p5.cursor(this.p5.CROSS);
        if (this.mapImage != null) {
          let f = this.mapImage;
          f.loadPixels();

          let rectX = Math.round((this.viewPort.x + p5.mouseX) / this.zoom);
          let rectY =
            this.loadImageHeight -
            Math.round(
              (this.viewPort.y - SCROLL_BAR_SIZE + p5.height - p5.mouseY) /
                this.zoom
            );
          let rectWidth = this.pencilSize;
          let rectHeight = this.pencilSize;

          let topLeft = { x: rectX, y: rectY };
          let topRight = { x: rectX + rectWidth, y: rectY };
          let bottomLeft = { x: rectX, y: rectY + rectHeight };
          let bottomRight = { x: rectX + rectWidth, y: rectY + rectHeight };

          let offsetX = this.pencilSize % 2 === 0 ? 0.5 : 0;
          let offsetY = this.pencilSize % 2 === 0 ? 0.5 : 0;
          //this.p5.cursor(this.p5.CROSS,xVal,y);
          if (
            !this.pointInsideRect(
              this.p5.mouseX,
              this.p5.mouseY,
              this.scrollBar.horizontal.offset + 42,
              this.p5.height - SCROLL_BAR_SIZE - 55,
              this.scrollBar.horizontal.size - 85,
              SCROLL_BAR_SIZE
            ) &&
            !this.pointInsideRect(
              this.p5.mouseX,
              this.p5.height - this.p5.mouseY,
              this.p5.width - SCROLL_BAR_SIZE - 5,
              this.scrollBar.vertical.offset + SCROLL_BAR_SIZE,
              SCROLL_BAR_SIZE,
              this.scrollBar.vertical.size
            )
          )
            this.p5.noCursor();
          else this.p5.cursor();
          let e = this.p5.color(this.drawColor);
          for (let i = -rectWidth / 2; i < rectWidth / 2; i++) {
            for (let j = -rectHeight / 2; j < rectHeight / 2; j++) {
              let pixelX = topLeft.x + i + offsetX;
              let pixelY = topLeft.y + j + offsetY;

              f.set(pixelX, pixelY, e);
            }
          }
          f.updatePixels();
        }
      }
      if (
        ["menuDrawRectangle", "menuDrawEllipse", "menuDrawTriangle"].includes(
          this.subMenuSelection
        )
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        if (this.store.miniMapFlag) return;
        let screenPosition = this.screenCoordinateFromMapCoordinate(
          this.drawingShape.Position
        );
        this.drawingShape.Size.width =
          (mouseX - screenPosition.x) *
          this.currentmap.data.MapInfo.MeterForPixel; //this.map.data.MapInfo.MeterForPixel;
        this.drawingShape.Size.height =
          (mouseY - screenPosition.y) *
          this.currentmap.data.MapInfo.MeterForPixel; //this.map.data.MapInfo.MeterForPixel;

        return;
      }

      if (this.subMenuSelection === "menuHandDrag") {
        this.handDragFlag = true;
        this.p5.cursor(this.p5.HAND);
        let dx, dy; //zoomout (zoom 0.5 ~~) shake
        dx = mouseX - this.mousePressPosition.x;
        dy = mouseY - this.mousePressPosition.y;

        this.moveViewPort(
          this.viewPort.x - dx * this.zoom,
          this.viewPort.y - dy * this.zoom
        );
        return;
      }

      if (
        this.subMenuSelection === "ctrlHandDrag" ||
        p5.keyIsDown(p5.CONTROL)
      ) {
        this.selectObject(null, false);
        this.handDragFlag = true;
        this.p5.cursor(this.p5.HAND);
        let dx, dy; //zoomout (zoom 0.5 ~~) shake
        dx = mouseX - this.mousePressPosition.x;
        dy = mouseY - this.mousePressPosition.y;

        this.moveViewPort(
          this.viewPort.x - dx * this.zoom,
          this.viewPort.y - dy * this.zoom
        );
        return;
      }

      if (
        (this.subMenuSelection == "menuSelect" ||
          this.isEdgeSettingModeEnabled) &&
        this.menuSelection !== "image"
      ) {
        if (this.store.miniMapFlag) return;
        if (this.selectedHandle != null) {
          this.onDragHandle(this.selectedHandle, mouseX, mouseY);
          return;
        }
        if (p5.keyIsDown(p5.SHIFT) || p5.keyIsDown(p5.CONTROL)) return;
        let objectAtMouse = this.getObjectAtMouse(pmouseX, pmouseY);

        if (this.guideLine != null) {
          this.guideLine.x2 = mouseX;
          this.guideLine.y2 = mouseY;
        } else {
          if (objectAtMouse == null && this.arrSelectedObjects.length == 0) {
            this.guideLine = {
              x: mouseX,
              y: mouseY,
              x2: mouseX,
              y2: mouseY,
              drawMeter: false,
            };
          }
        }
        if (this.arrSelectedObjects.length == 0) return;

        this.draggingObject = this.arrSelectedObjects.slice();
        if (this.mouseMoveOffset == null) {
          this.dragStartPosition = { x: mouseX, y: mouseY };
          let firstObject = this.arrSelectedObjects[0];
          let screenPosition;
          if (Array.isArray(firstObject.Position)) {
            screenPosition = this.screenCoordinateFromMapCoordinate(
              firstObject.Position[0]
            );
          } else {
            screenPosition = this.screenCoordinateFromMapCoordinate(
              firstObject.Position
            );
          }
          this.mouseMoveOffset = {
            x: screenPosition.x - mouseX,
            y: screenPosition.y - mouseY,
          };
        }

        let xDiff = null;
        let yDiff = null;

        for (let aObject of this.arrSelectedObjects) {
          let isPolygonLine = ["polygon", "line"].includes(aObject.shape_type); //checking whether it is "polygon" or "line"

          if (xDiff === null && !isPolygonLine) {
            const screenPosition = Array.isArray(aObject.Position)
              ? this.screenCoordinateFromMapCoordinate(aObject.Position[0])
              : this.screenCoordinateFromMapCoordinate(aObject.Position);
            xDiff = mouseX + this.mouseMoveOffset.x - screenPosition.x;
            yDiff = mouseY + this.mouseMoveOffset.y - screenPosition.y;
          }
          if (xDiff === null && isPolygonLine && this.store.dragMove) {
            //condition apply for "polygon" and "line"
            const screenPosition = Array.isArray(aObject.Position)
              ? this.screenCoordinateFromMapCoordinate(aObject.Position[0])
              : this.screenCoordinateFromMapCoordinate(aObject.Position);
            xDiff = mouseX + this.mouseMoveOffset.x - screenPosition.x;
            yDiff = mouseY + this.mouseMoveOffset.y - screenPosition.y;
          }

          if (Array.isArray(aObject.Position)) {
            for (let point of aObject.Position) {
              point.x = point.x + xDiff * this.meterForPixel;
              point.y = point.y + yDiff * this.meterForPixel;
              for (let handle of this.arrHandles) {
                if (handle.target == point) {
                  handle.x = aObject.Position.x;
                  handle.y = aObject.Position.y;
                }
              }
            }
            this.calcBoundingBox(aObject);
          } else {
            if (this.store.dragMove) {
              // check whether it dragging from Rightdrawer component
              aObject.Position.x =
                aObject.Position.x +
                xDiff * this.currentmap.data.MapInfo.MeterForPixel;
              aObject.Position.y =
                aObject.Position.y +
                yDiff * this.currentmap.data.MapInfo.MeterForPixel;
            }
            for (let handle of this.arrHandles) {
              if (handle.target == aObject) {
                if (handle.type == "bottomLeft") {
                  handle.x = aObject.Position.x;
                  handle.y = aObject.Position.y;
                } else if (handle.type == "bottomRight") {
                  handle.x = aObject.Position.x + aObject.Size.width - 1;
                  handle.y = aObject.Position.y;
                } else if (handle.type == "topRight") {
                  handle.x = aObject.Position.x + aObject.Size.width - 1;
                  handle.y = aObject.Position.y + aObject.Size.height - 1;
                } else if (handle.type == "topLeft") {
                  handle.x = aObject.Position.x;
                  handle.y = aObject.Position.y + aObject.Size.height - 1;
                }
              }
            }
          }
        }
      }
    },

    afterScrollBarMoved(direction) {
      if (direction === "horizontal") {
        let viewPortX =
          (this.scrollBar.horizontal.offset * this.loadImageWidth * this.zoom) /
          (this.p5.width - SCROLL_BAR_SIZE);
        this.moveViewPort(viewPortX, this.viewPort.y);
      } else {
        let viewPortY =
          (this.scrollBar.vertical.offset * this.loadImageHeight * this.zoom) /
          (this.p5.height - SCROLL_BAR_SIZE);
        this.moveViewPort(this.viewPort.x, viewPortY);
      }
    },

    moveViewPort(x, y) {
      if (!this.p5) return; //p5 missing error skip

      if (this.viewPort.width > this.p5.width) {
        this.viewPort.x = x;
        if (this.viewPort.x < 0) this.viewPort.x = 0;
        if (
          this.viewPort.x >
          this.loadImageWidth * this.zoom - this.p5.width + SCROLL_BAR_SIZE
        )
          this.viewPort.x =
            this.loadImageWidth * this.zoom - this.p5.width + SCROLL_BAR_SIZE;
      }
      if (this.viewPort.height > this.p5.height) {
        this.viewPort.y = y;
        if (this.viewPort.y < 0) this.viewPort.y = 0;
        if (this.viewPort.y > this.loadImageHeight * this.zoom - this.p5.height)
          this.viewPort.y = this.loadImageHeight * this.zoom - this.p5.height;
      }

      console.log("   moveViewPort after: ", this.viewPort.x, this.viewPort.y);
      this.recalcRuler();
      this.recalcScrollbar();
    },

    recalcScrollbar() {
      this.scrollBar.horizontal.offset =
        (this.viewPort.x / (this.loadImageWidth * this.zoom)) *
        (this.p5.width - SCROLL_BAR_SIZE);
      this.scrollBar.vertical.offset =
        (this.viewPort.y / (this.loadImageHeight * this.zoom)) * this.p5.height;

      if (this.loadImageWidth * this.zoom > this.p5.width - SCROLL_BAR_SIZE)
        this.scrollBar.horizontal.size =
          ((this.p5.width - SCROLL_BAR_SIZE) /
            (this.loadImageWidth * this.zoom)) *
          (this.p5.width - SCROLL_BAR_SIZE);
      else this.scrollBar.horizontal.size = 0;
      if (this.loadImageHeight * this.zoom > this.p5.height - SCROLL_BAR_SIZE)
        this.scrollBar.vertical.size =
          ((this.p5.height - SCROLL_BAR_SIZE) /
            (this.loadImageHeight * this.zoom)) *
          (this.p5.height - SCROLL_BAR_SIZE);
      else this.scrollBar.vertical.size = 0;

      this.limitScrollBar("horizontal");
      this.limitScrollBar("vertical");
    },

    limitScrollBar(direction) {
      if (direction === "horizontal") {
        if (this.scrollBar.horizontal.offset < 0)
          this.scrollBar.horizontal.offset = 0;
        if (
          this.scrollBar.horizontal.offset + this.scrollBar.horizontal.size >
          this.p5.width - SCROLL_BAR_SIZE
        )
          this.scrollBar.horizontal.offset =
            this.p5.width - SCROLL_BAR_SIZE - this.scrollBar.horizontal.size;
      } else {
        if (this.scrollBar.vertical.offset < 0)
          this.scrollBar.vertical.offset = 0;
        if (
          this.scrollBar.vertical.offset + this.scrollBar.vertical.size >
          this.p5.height
        ) {
          this.scrollBar.vertical.offset =
            this.p5.height - this.scrollBar.vertical.size;
        }
      }
      this.zoomFromMouse = false;
    },
    async mouseReleased(p5, mouseX, mouseY) {
      this.store.dragScroll.drag = false;
      this.store.dragScroll.press = false;
      if (this.stopSelection) {
        this.stopSelection = false;
      }

      if (this.isDragging) {
        this.isDragging = false;
      }

      if (this.getObjectType(mouseX, mouseY) === this.$t("common.node"))
        this.store.updateSelectedEdgeGroup = false;

      if (this.handDragFlag) {
        this.p5.cursor(this.p5.ARROW);
        this.handDragFlag = false;
      }

      if (this.store.elevatorMode) {
        //For elevator update
        this.movingRotationBtn();
      }

      console.log(this.isLineDrawing, this.isPainting, this.subMenuSelection);

      if (!this.isLineDrawing && this.dragflag == true) {
        const imgData = this.imageContext.getImageData(
          0,
          0,
          this.imageContext.canvas.width,
          this.imageContext.canvas.height
        );
        let image = this.getImageAfterDrawing(this.imageContext, imgData);
        this.undoStack.push(image);
        this.dragflag = false;
        return;
      }
      //selection point
      if (
        this.subMenuSelection == "menuShapeSelect" &&
        this.isSelecting &&
        !this.store.objectFlag
      ) {
        let result = this.ctrlClickFun();
        if (result === "exit") return;
        console.log("SELECTION OKAY");
        if (mouseX > this.selection.x && mouseY > this.selection.y) {
          this.selection.x = mouseX - this.selection.w;
          this.selection.y = this.selection.y + this.selection.h;
        }
        if (mouseX < this.selection.x && mouseY > this.selection.y) {
          this.selection.x = this.selection.x - this.selection.w * -1;
          this.selection.y = this.selection.y + this.selection.h;
        }
        if (mouseX < this.selection.x && mouseY < this.selection.y) {
          this.selection.x = this.selection.x - this.selection.w * -1;
          this.selection.y = this.selection.y;
        }
        this.selection.w =
          this.selection.w < 0 ? this.selection.w * -1 : this.selection.w;
        this.selection.h =
          this.selection.h > 0 ? this.selection.h * -1 : this.selection.h;
        console.log("selection coordinates: ", this.selection);
      }
      //Line Drawing
      if (this.isLineDrawing) {
        let clone = { ...this.linePoints };
        this.arrLine.push(clone);
        //this.arrLine.pop();
        if (this.arrLine.length === 1) {
          this.arrAllLines.push(this.arrLine);
        }
        console.log("LIne Drawing in pencil menu", this.arrLine);
        this.drawSingleLineSequence(this.imageContext, this.arrLine);
      }
      //---------------------
      this.mouseMoveOffset = null;
      this.mousePressPosition = null;
      this.mouseDraggedPosition = null;
      if (
        ["menuDrawRectangle", "menuDrawEllipse", "menuDrawTriangle"].includes(
          this.subMenuSelection
        )
      ) {
        if (this.drawingShape != null) {
          if (this.drawingShape.Size.width < 0) {
            this.drawingShape.Size.width *= -1;
            this.drawingShape.Position.x -= this.drawingShape.Size.width;
          }
          if (this.drawingShape.Size.height < 0) {
            this.drawingShape.Size.height *= -1;
            this.drawingShape.Position.y -= this.drawingShape.Size.height;
          }
          this.addObjects([this.drawingShape], true);
          console.log(
            "drawingShape",
            JSON.stringify(this.drawingShape, null, 4)
          );
          this.drawingShape = null;
          this.store.miniMapValue = null;
        }
        this.$emit("backToMenuSelect");
        return;
      } else if (
        (this.subMenuSelection == "menuSelect" ||
          this.isEdgeSettingModeEnabled) &&
        this.menuSelection !== "image"
      ) {
        if (this.draggingObject.length > 0) {
          //-------------------------------------------------------------
          // POI Validation after drag
          for await (const element of this.arrSelectedObjects) {
            if (element.hasOwnProperty("cpId")) {
              this.finalPOIPos.set(element.cpId, element.Position);
            }
          }

          if (this.finalPOIPos.size) {
            this.getCtxData();
            let radius = this.DIAMETER / this.meterForPixel / 2;
            for await (const [key, value] of this.finalPOIPos) {
              let screenCoordinate =
                this.screenCoordinateFromMapCoordinate(value);

              for (let theta = 0; theta <= 360; theta += 10) {
                let arcCordinate = this.circleXY(radius, theta);
                this.getPixelValAtPos(
                  Math.floor(screenCoordinate.x),
                  this.mapImage.height - Math.floor(screenCoordinate.y),
                  this.imageContextForValidation
                );
                //let arcCordinate = this.circleXY(radius, theta);
                this.getPixelValAtPos(
                  Math.floor(screenCoordinate.x + arcCordinate.x),
                  this.mapImage.height -
                    Math.floor(screenCoordinate.y + arcCordinate.y),
                  this.imageContextForValidation
                );
                this.getShapeDataAtPos(
                  Math.floor(screenCoordinate.x + arcCordinate.x),
                  this.mapImage.height -
                    Math.floor(screenCoordinate.y + arcCordinate.y),
                  this.shapeContextForValidation
                );
                // this.getShapeDataAtPos(
                //  Math.floor(screenCoordinate.x ),
                //  this.mapImage.height - Math.floor(screenCoordinate.y),
                //  this.shapeContextForValidation
                // );
                //console.log('(x, y) = ' + '(' + arcCordinate.x + ', ' + arcCordinate.y + ') for theta=' + theta);

                //console.log("theta value is:",angle);

                if (
                  this.combinedPixelData === 0 ||
                  this.combinedPixelData === 450 ||
                  this.combinedShapeData === "00255"
                ) {
                  this.changePOIPos.push(key);
                }
              }
            }
            if (this.changePOIPos.length) {
              for await (const element of this.arrSelectedObjects) {
                if (element.hasOwnProperty("cpId")) {
                  if (this.changePOIPos.includes(element.cpId)) {
                    Object.assign(
                      element.Position,
                      this.initialPOIPos.get(element.cpId)
                    );
                  }
                }
              }
            }
          }
          //-------------------------------------------------------------

          // Undo-Redo POI Update after validation
          if (this.changePOIPos.length) {
            this.draggingObj = this.draggingObject.filter((el) => {
              if (!el.cpId) return true;
              return !this.changePOIPos.includes(el.cpId);
            });
          } else {
            this.draggingObj = this.draggingObject;
          }

          let x_move = mouseX - this.dragStartPosition.x;
          let y_move = mouseY - this.dragStartPosition.y;
          console.log("update node undo", mouseX, mouseY);
          console.log("dragged object pos: ", this.draggingObj);
          if ((x_move != 0 || y_move != 0) && this.draggingObj) {
            this.addUndo({
              action: "move",
              x_move: x_move * this.meterForPixel,
              y_move: y_move * this.meterForPixel,
              objects: this.draggingObj,
            });
          }

          this.changePOIPos = [];
          this.finalPOIPos.clear();
        }
        this.checkMultiSelect();
        this.dragSelectEdges();
        this.guideLine = null;
        this.draggingObject = [];
        this.draggingObj = [];
        if (this.selectedHandle != null) {
          this.calcBoundingBox(this.selectedHandle.target);
          if (this.selectedHandle.target.Size !== null) {
            if (this.selectedHandle.target.Size.width < 0) {
              this.selectedHandle.target.Size.width *= -1;
              this.selectedHandle.target.Position.x -=
                this.selectedHandle.target.Size.width;
            }
            if (this.selectedHandle.target.Size.height < 0) {
              this.selectedHandle.target.Size.height *= -1;
              this.selectedHandle.target.Position.y -=
                this.selectedHandle.target.Size.height;
            }
          }

          this.selectedHandle = null;
        }
      }

      if (this.isSelectingObject && this.arrSelectedObjects.length == 1) {
        this.isSelectingObject = false;
        this.attachHandles();
      }
    },
    keyPressed(p5, mouseX, mouseY) {
      console.log("p5, mouseX, mouseY", p5, mouseX, mouseY);
      if (p5.keyCode === p5.ESCAPE && this.store.addProperty) {
        this.store.addProperty = false;
        return;
      }
      // POI validation
      //   if(this.arrSelectedObjects.length == 1 && this.arrSelectedObjects[0].hasOwnProperty("cpId")) {

      //   this.takeShapeSnapshot = true;
      //   let screenCoord = this.screenCoordinateFromMapCoordinate(this.poiPosition);
      //   this.getPixelValAtPos(
      //     Math.floor(screenCoord.x), this.mapImage.height - Math.floor(screenCoord.y), this.imageContextForValidation
      //   ); // change 2000 to this.mapImage.height
      //   this.getShapeDataAtPos(
      //     Math.floor(screenCoord.x), this.mapImage.height - Math.floor(screenCoord.y), this.shapeContextForValidation
      //   );
      //   if (
      //     this.combinedPixelData === 0 ||
      //     this.combinedPixelData === 450 ||
      //     this.combinedShapeData == "00255"
      //   ) {
      //     console.warn("NOT MOVING POI!!");
      //     return;
      //   }
      //   console.warn("MOVING POI!!");
      // }
      //--------------

      if (p5.keyCode == p5.CONTROL) {
        this.isCtrlPressed = true;
        this.store.dragScroll.ctrl = true;
      }
      //copy paste
      if (this.isCtrlPressed && p5.key == "c") {
        this.showSelection = false;
        this.copy(this.imageContext);
      }

      if (this.isCtrlPressed && ["ArrowRight", "ArrowLeft"].includes(p5.key)) {
        this.$emit("selectedObjXEnter", p5);
      }

      if (this.isCtrlPressed && ["ArrowUp", "ArrowDown"].includes(p5.key)) {
        this.$emit("selectedObjYEnter", p5);
      }

      //copy paste
      if (this.isCtrlPressed && p5.key == "v") {
        let ctx = this.imageContext;
        function getImageURL(imgData, width, height) {
          let canvas = document.createElement("canvas");
          let newCtx = canvas.getContext("2d");
          canvas.width = width;
          canvas.height = height;
          newCtx.putImageData(imgData, 0, 0);
          return canvas.toDataURL(); //image URL
        }
        let previousImageData = ctx.getImageData(
          0,
          0,
          ctx.canvas.width,
          ctx.canvas.height
        );
        var PreviousImage = new Image();
        PreviousImage.src = getImageURL(previousImageData, 2000, 2000);
        this.previousStoredImages.push(PreviousImage);
        this.pasteIndex++;
        this.stopSelection = true;
        this.paste(this.imageContext);
        this.isMoved = false;
      }
      if (
        p5.keyCode === p5.SHIFT &&
        ["menuPencil"].includes(this.subMenuSelection)
      ) {
        console.log("line drawing starts");
        this.isLineDrawing = true;
        this.arrLine = [];
        this.stopPencil = true;
      }

      if (p5.keyCode === p5.ESCAPE) {
        this.store.createPoiLineUp = false;
        if (!this.store.escTriger && this.store.drawer) {
          this.store.escTriger = true;
          this.store.drawerSelection = this.store.selectedProperty = null;
        }

        if (this.store.reisFlag) {
          this.elevator = false;
          this.store.reisFlag = false; //close the REIS popup
        }

        if (this.store.edgeHighlightFlag) this.store.edgeHighlightFlag = false;

        if (p5.keyCode === p5.ESCAPE) {
          if (!this.store.addProperty) this.escFunctionCalling(p5);

          if (
            ["menuDrawEllipse", "menuDrawRectangle"].includes(
              this.subMenuSelection
            )
          ) {
            this.store.miniMapValue = "menuSelect";
          }
        }
        //COPY PASTE SELECTION
        if (this.subMenuSelection === "menuShapeSelect") {
          this.isSelecting = false;
          this.showSelection = false;
        }
        if (
          this.subMenuSelection === "menuDrawLine" ||
          this.subMenuSelection === "menuDrawPolygon"
        ) {
          this.finishLineDraw();
          this.$emit("backToMenuSelect");
        } else if (this.subMenuSelection === "menuSelect") {
          this.selectObject(null, false);
        } else if (this.subMenuSelection === "newNode") {
          this.stopNewNode();
        } else if (this.subMenuSelection === "newPoi") {
          this.stopNewPoi();
        } else if (this.subMenuSelection === "AutoCreatePOI") {
          this.store.autoCreateBool = false;
          this.stopNewPoi();
        } else {
          this.$emit("backToMenuSelect");
        }

        return;
      } else if (p5.keyCode == p5.BACKSPACE || p5.keyCode == p5.DELETE) {
        console.log("delete", this.arrSelectedObjects.length);
        // if (this.getObjectType(this.arrSelectedObjects) === this.$t('common.node')) {
        //   console.log("Delete Node");
        // }
        if (this.arrSelectedObjects.length === 0) return;
        if (
          this.getObjectType(this.arrSelectedObjects) ===
          this.$t("common.shape")
        ) {
          console.log("shape");
        }
        if (document.activeElement.tagName.toLowerCase() != "input")
          this.deleteObjects(this.arrSelectedObjects, true);
      }

      if (
        this.subMenuSelection == "menuSelect" &&
        document.activeElement.tagName.toLowerCase() != "input"
      ) {
        let move = 1;
        if (this.p5 != null && this.p5.keyIsDown(this.p5.SHIFT)) move = 10;

        if (p5.keyCode == p5.LEFT_ARROW) {
          this.moveSelectedObjectBy(-move, 0, p5.mouseX, p5.mouseY);
        } else if (p5.keyCode == p5.RIGHT_ARROW) {
          this.moveSelectedObjectBy(move, 0, p5.mouseX, p5.mouseY);
        } else if (p5.keyCode == p5.UP_ARROW) {
          this.moveSelectedObjectBy(0, move, p5.mouseX, p5.mouseY);
        } else if (p5.keyCode == p5.DOWN_ARROW) {
          this.moveSelectedObjectBy(0, -move, p5.mouseX, p5.mouseY);
        }
      }
    },
    moveSelectedObjectBy(x, y, mouseX, mouseY) {
      for (let aObject of this.arrSelectedObjects) {
        if (Array.isArray(aObject.Position)) {
          for (let point of aObject.Position) {
            point.x += x;
            point.y += y;
          }
          this.calcBoundingBox(aObject);
        } else {
          console.log("mouseX", p5.mouseX);
          console.log("mouseY", p5.mouseY);
          this.getPixelValAtPos(
            Math.floor(mouseX),
            this.mapImage.height - Math.floor(mouseY),
            this.imageContextForValidation
          ); // change 2000 to this.mapImage.height

          // this.takeShapeSnapshot = true;
          this.getCtxData();

          this.getShapeDataAtPos(
            Math.floor(mouseX),
            this.mapImage.height - Math.floor(mouseY),
            this.shapeContextForValidation
          );
          if (
            this.combinedPixelData === 0 ||
            this.combinedPixelData === 450 ||
            this.combinedShapeData == "00255"
          ) {
            // console.warn("Moving out of the area");
            return;
          } else {
            aObject.Position.x += x;
            aObject.Position.y += y;
          }
        }
        if (this.getObjectType(aObject) == this.$t("common.shape")) {
          for (let handle of this.arrHandles) {
            handle.x += x;
            handle.y += y;
          }
        }
      }
    },
    keyReleased(p5) {
      if (p5.keyCode === p5.CONTROL) {
        this.isCtrlPressed = false;
        this.store.dragScroll.ctrl = false;
      }
      if (
        p5.keyCode === p5.SHIFT &&
        ["menuPencil"].includes(this.subMenuSelection)
      ) {
        console.log("line drawing ends");
        this.isLineEnd = true;
        this.isLineDrawing = false;
        this.arrLine = null;
        this.guideLine = null;
        this.stopPencil = false;
      }
    },
  },
  created() {},
  mounted() {
    const { sessionId } = this.$route.query;
    if (!sessionId) {
      return;
    }
    let parentThis = this;
    console.log("parentThis is ->>>", parentThis);
    let p5Handler = function (p5) {
      p5.setup = () => {
        parentThis.container = parentThis.$refs.p5Container;
        let container = parentThis.container;
        let width = container.offsetWidth;
        let height = container.offsetHeight;
        console.log("createCanvas", width, height);
        let canvas = p5.createCanvas(width, height);
        console.log("canvas ->>", canvas);
        canvas.parent(container);

        // Loading images here from local server. To be changed later.
        parentThis.imageNode = p5.loadImage(
          `${
            process.env.NODE_ENV === "development"
              ? devmodeImageUrl()
              : getUrl()
          }images/icon_node_normal.png`
        );
        parentThis.imageNodePOI = p5.loadImage(
          `${
            process.env.NODE_ENV === "development"
              ? devmodeImageUrl()
              : getUrl()
          }images/icon_node_poi.png`
        );
        parentThis.imageNodeElevator = p5.loadImage(
          `${
            process.env.NODE_ENV === "development"
              ? devmodeImageUrl()
              : getUrl()
          }images/icon_node_elevator.png`
        );
        parentThis.imageRotate = p5.loadImage(
          `${
            process.env.NODE_ENV === "development"
              ? devmodeImageUrl()
              : getUrl()
          }images/ev_poi_rotate_button.svg`
        );
      };
      //parentThis.store.setLoadImage(true);

      p5.draw = () => {
        parentThis.draw(p5);
      };

      p5.keyPressed = (event) => {
        parentThis.keyPressed(
          p5,
          (+parentThis.viewPort.x + p5.mouseX) / parentThis.zoom,
          (+parentThis.viewPort.y - SCROLL_BAR_SIZE + p5.height - p5.mouseY) /
            parentThis.zoom
        );
      };
      p5.keyReleased = (event) => {
        parentThis.keyReleased(p5);
      };

      p5.mouseMoved = () => {
        if (parentThis.isOutOfCanvas(p5.mouseX, p5.mouseY)) return;
        if (parentThis.store.dragScroll.ctrl) {
          parentThis.store.dragScroll.press = true;
        }
        parentThis.mouseMoved(
          p5,
          (parentThis.viewPort.x + p5.mouseX) / parentThis.zoom,
          (parentThis.viewPort.y - SCROLL_BAR_SIZE + p5.height - p5.mouseY) /
            parentThis.zoom,
          p5.pmouseX,
          p5.height - p5.pmouseY
        );
      };

      p5.mouseDragged = (event) => {
        if (parentThis.selectedScrollbar != null) {
          if (parentThis.selectedScrollbar.name === "horizontal") {
            parentThis.selectedScrollbar.offset =
              parentThis.selectedScrollbar.pressedOffset +
              parentThis.p5.mouseX -
              parentThis.selectedScrollbar.pressedMousePosition.x;

            parentThis.afterScrollBarMoved("horizontal");
          } else {
            parentThis.selectedScrollbar.offset =
              parentThis.selectedScrollbar.pressedOffset -
              (parentThis.p5.mouseY -
                parentThis.selectedScrollbar.pressedMousePosition.y);
            console.log(
              "parentThis.selectedScrollbar.offset",
              parentThis.selectedScrollbar.offset
            );
            parentThis.afterScrollBarMoved("vertical");
          }
          return;
        }

        if (parentThis.isOutOfCanvas(p5.mouseX, p5.mouseY)) return;

        parentThis.mouseDragged(
          p5,
          (parentThis.viewPort.x + p5.mouseX) / parentThis.zoom,
          (parentThis.viewPort.y - SCROLL_BAR_SIZE + p5.height - p5.mouseY) /
            parentThis.zoom,
          p5.pmouseX,
          p5.height - p5.pmouseY
        );
      };

      p5.mousePressed = (event) => {
        if (parentThis.isDialogOpen()) return;
        if (parentThis.store.objectFlag) return; //control the click on the main menu and option panel
        let scrollbar = parentThis.getScrollbarAtMouse();
        if (scrollbar != null) {
          parentThis.selectedScrollbar = scrollbar;
          parentThis.selectedScrollbar.pressedOffset = scrollbar.offset;
          parentThis.selectedScrollbar.pressedMousePosition = {
            x: p5.mouseX,
            y: p5.mouseY,
          };
          return;
        } else {
          let scrollRail = parentThis.getScrollRailAtMouse();
          if (scrollRail === "horizontal_left") {
            parentThis.scrollBar.horizontal.offset -=
              parentThis.scrollBar.horizontal.size;
            parentThis.afterScrollBarMoved("horizontal");
            return;
          } else if (scrollRail === "horizontal_right") {
            parentThis.scrollBar.horizontal.offset +=
              parentThis.scrollBar.horizontal.size;
            parentThis.afterScrollBarMoved("horizontal");
            return;
          } else if (scrollRail === "vertical_up") {
            parentThis.scrollBar.vertical.offset +=
              parentThis.scrollBar.vertical.size;
            parentThis.afterScrollBarMoved("vertical");
            return;
          } else if (scrollRail === "vertical_down") {
            parentThis.scrollBar.vertical.offset -=
              parentThis.scrollBar.vertical.size;
            parentThis.afterScrollBarMoved("vertical");
            return;
          } else {
            console.log("no scroll area");
          }
        }

        if (parentThis.isOutOfCanvas(p5.mouseX, p5.mouseY)) return;
        parentThis.mousePressed(
          p5,
          (+parentThis.viewPort.x + p5.mouseX) / parentThis.zoom,
          (+parentThis.viewPort.y - SCROLL_BAR_SIZE + p5.height - p5.mouseY) /
            parentThis.zoom,
          p5.pmouseX,
          p5.height - p5.pmouseY
        );
      };

      p5.mouseWheel = (event) => {
        if (
          parentThis.p5.keyIsDown(parentThis.p5.SHIFT) ||
          parentThis.p5.keyIsDown(parentThis.p5.CONTROL) ||
          parentThis.p5.keyIsDown(parentThis.p5.ALT)
        ) {
          parentThis.store.objectFlag = false;
        }
        if (parentThis.isDialogOpen() || parentThis.store.objectFlag) return;

        if (
          p5.mouseX > p5.width ||
          p5.mouseX < 0 ||
          p5.mouseY > p5.height ||
          p5.mouseY < 0
        )
          return;
        event.preventDefault();
        event.stopPropagation();

        if (
          !parentThis.store.objectFlag &&
          !parentThis.p5.keyIsDown(parentThis.p5.SHIFT) &&
          !parentThis.p5.keyIsDown(parentThis.p5.CONTROL) &&
          !parentThis.p5.keyIsDown(parentThis.p5.ALT)
        ) {
          parentThis.scrollBar.vertical.offset += -(event.delta / 6);
          parentThis.afterScrollBarMoved("vertical");
        }

        if (parentThis.p5.keyIsDown(parentThis.p5.CONTROL)) {
          console.log("zoom wheel", event);

          if (!parentThis.store.isMouseOver) {
            // Calculate the mouse position in world coordinates before the zoom
            const prevMouseX =
              (parentThis.viewPort.x + p5.mouseX) / parentThis.zoom;
            const prevMouseY =
              (parentThis.viewPort.y -
                SCROLL_BAR_SIZE +
                p5.height -
                p5.mouseY) /
              parentThis.zoom;

            // Change the zoom level based on the mouse wheel event
            if (event.delta > 0) {
              // Zoom in
              if (parentThis.zoom / 1.1 > 0.68) {
                parentThis.zoom /= 1.1;
              } else if (
                parentThis.zoom / 1.1 <= 0.68 &&
                parentThis.zoom > 0.68
              ) {
                parentThis.zoom = 0.68;
              }
            } else {
              // Zoom out
              if (parentThis.zoom * 1.1 < 12.0) {
                parentThis.zoom *= 1.1;
              } else if (
                parentThis.zoom * 1.1 >= 12.0 &&
                parentThis.zoom < 12.0
              ) {
                parentThis.zoom = 12.0;
              }
            }

            // Calculate the mouse position in world coordinates after the zoom
            const newMouseX =
              (parentThis.viewPort.x + p5.mouseX) / parentThis.zoom;
            const newMouseY =
              (parentThis.viewPort.y -
                SCROLL_BAR_SIZE +
                p5.height -
                p5.mouseY) /
              parentThis.zoom;

            // Adjust the view offset based on the difference between the two positions
            parentThis.viewPort.x += (prevMouseX - newMouseX) * parentThis.zoom;
            parentThis.viewPort.y += (prevMouseY - newMouseY) * parentThis.zoom;
          }
        } else {
          if (
            p5.mouseX > p5.width ||
            p5.mouseX < 0 ||
            p5.mouseY > p5.height ||
            p5.mouseY < 0
          )
            return;

          if (
            !parentThis.store.objectFlag &&
            !parentThis.p5.keyIsDown(parentThis.p5.SHIFT) &&
            !parentThis.p5.keyIsDown(parentThis.p5.CONTROL) &&
            !parentThis.p5.keyIsDown(parentThis.p5.ALT)
          ) {
            parentThis.scrollBar.vertical.offset += -(event.delta / 6);
            parentThis.afterScrollBarMoved("vertical");
          }
        }
      };

      p5.mouseReleased = () => {
        parentThis.selectedScrollbar = null;
        if (parentThis.isOutOfCanvas(p5.mouseX, p5.mouseY)) {
          if (parentThis.subMenuSelection == "menuSelect") {
            parentThis.guideLine = null;
          }
          return;
        }
        parentThis.mouseReleased(
          p5,
          (parentThis.viewPort.x + p5.mouseX) / parentThis.zoom,
          (parentThis.viewPort.y - SCROLL_BAR_SIZE + p5.height - p5.mouseY) /
            parentThis.zoom
        );
      };

      p5.windowResized = (event) => {
        parentThis.windowResized(p5);
      };
    };

    let P5 = p5;
    this.p5 = new P5(p5Handler);
    this.p5.angleMode(this.p5.DEGREES);
    this.p5.ellipseMode(this.p5.CENTER);
    this.p5.rectMode(this.p5.CENTER);
    this.p5.imageMode(this.p5.CENTER);
    this.p5.textAlign(this.p5.CENTER, this.p5.CENTER);
    this.p5.noSmooth();
    this.colorGuideLine = this.p5.color("#FF0000");

    this.$nextTick(() => {
      this.recalcViewPort();
    });
    this.$nextTick(() => {
      setTimeout(() => {
        this.loadMapImage(() => {
          this.$emit("loadingDone", this);
          //reset center margin
          this.styleObject.left = "30px";
          this.styleObject.top = "29px";
          this.styleObject.right = "0px";
          this.styleObject.bottom = "0px";
        });
      }, 1500);
    });
  },
  beforeUpdate() {
    this.recalcViewPort();
  },
  updated() {},
  activated() {
    console.log("checking", this.currentMap);

    // this.$nextTick(() => {
    //   this.loadMapImage(() => {
    //   console.log("testegfbsbdfbjksn");
    //   this.$emit("loadingDone", this);
    //   //reset center margin
    //   this.styleObject.left = "30px";
    //   this.styleObject.top = "29px";
    //   this.styleObject.right = "0px";
    //   this.styleObject.bottom = "0px";
    // });
    // });
  },
  deactivated() {},
  beforeUnmount() {
    if (this.p5 != null) {
      this.p5.noCanvas();
      this.p5.clear();
    }
    delete this.p5;
  },
  unmounted() {
    this.p5 = null;
  },
};
</script>

<style scoped>
#p5root {
  height: 100%;
  width: 100%;
}
#p5Container {
  height: 100%;
  width: 100%;
}
.highlight-green {
  background-color: green;
}
</style>

