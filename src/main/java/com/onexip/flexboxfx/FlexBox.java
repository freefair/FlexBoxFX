package com.onexip.flexboxfx;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.apache.commons.collections4.iterators.ReverseListIterator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by TB on 11.10.16.
 */
public class FlexBox extends Pane {
	private SimpleDoubleProperty horizontalSpace = new SimpleDoubleProperty(0);
	private SimpleDoubleProperty verticalSpace = new SimpleDoubleProperty(0);
	private SimpleObjectProperty<FlexBoxDirection> direction = new SimpleObjectProperty<FlexBoxDirection>(FlexBoxDirection.ROW);
	private double computedMinHeight;
	private boolean performingLayout = false;
	private Orientation bias;
	private boolean biasDirty = true;
	private boolean verbose = false;
	private static final String ORDER_CONSTRAINT = "flexbox-order";
	private static final String GROW_CONSTRAINT = "flexbox-grow";

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public FlexBoxDirection getDirection() {
		return direction.get();
	}

	public SimpleObjectProperty<FlexBoxDirection> directionProperty() {
		return direction;
	}

	public void setDirection(FlexBoxDirection direction) {
		this.direction.set(direction);
	}

	public double getHorizontalSpace() {
		return horizontalSpace.get();
	}

	public SimpleDoubleProperty horizontalSpaceProperty() {
		return horizontalSpace;
	}

	public void setHorizontalSpace(double horizontalSpace) {
		this.horizontalSpace.set(horizontalSpace);
	}

	public double getVerticalSpace() {
		return verticalSpace.get();
	}

	public SimpleDoubleProperty verticalSpaceProperty() {
		return verticalSpace;
	}

	public void setVerticalSpace(double verticalSpace) {
		this.verticalSpace.set(verticalSpace);
	}

	private HashMap<Integer, FlexBoxRow> grid = new HashMap<Integer, FlexBoxRow>();


	/**
	 * By default, flex items are laid out in the source order.
	 * However, the order property controls the order in which they appear in the flex container.
	 *
	 * @param child the child of an FlexBox
	 * @param value the order in which the child appear in the flex container
	 */
	public static void setOrder(Node child, int value) {
		setConstraint(child, ORDER_CONSTRAINT, value);
	}

	public static int getOrder(Node child) {
		Object constraint = getConstraint(child, ORDER_CONSTRAINT);
		if (constraint == null) {
			return 0;
		} else {
			return (int) constraint;
		}
	}

	/**
	 * This defines the ability for a flex item to grow if necessary.
	 * It accepts a unitless value that serves as a proportion.
	 * It dictates what amount of the available space inside the flex container the item should take up.
	 * If all items have flex-grow set to 1, the remaining space in the container will be distributed equally to all children.
	 * If one of the children has a value of 2, the remaining space would take up twice as much space as the others (or it will try to, at least).
	 *
	 * @param child the child of an FlexBox
	 * @param value grow proportion
	 */
	public static void setGrow(Node child, double value) {
		setConstraint(child, GROW_CONSTRAINT, value);
	}

	public static double getGrow(Node child) {
		Object o = getConstraint(child, GROW_CONSTRAINT);
		if (o == null) {
			return 1;
		} else {
			return (double) o;
		}
	}

	static void setConstraint(Node node, Object key, Object value) {
		if (value == null) {
			node.getProperties().remove(key);
		} else {
			node.getProperties().put(key, value);
		}
		if (node.getParent() != null) {
			node.getParent().requestLayout();
		}
	}

	static Object getConstraint(Node node, Object key) {
		if (node.hasProperties()) {
			Object value = node.getProperties().get(key);
			if (value != null) {
				return value;
			}
		}
		return null;
	}


	@Override
	protected double computeMinHeight(double width) {
		//return super.computeMinHeight(width);
		return computedMinHeight;
	}

	@Override
	protected double computePrefHeight(double width) {
//        return super.computePrefHeight(width);
		return computedMinHeight;
	}

	@Override
	protected void layoutChildren() {
		//System.out.println("layoutChildren");
		//super.layoutChildren();

		long timeStart = System.nanoTime();

		grid.clear();

		List<FlexBoxItem> nodesList = new ArrayList<>();

		/**
		 * First we transform all Nodes to a FlexBoxItem for caching purposes.
		 */
		for (Node node : getManagedChildren()) {
			if (node.isVisible()) {
				nodesList.add(new FlexBoxItem(node));
			}
		}

		if (getDirection().equals(FlexBoxDirection.ROW) || getDirection().equals(FlexBoxDirection.ROW_REVERSE))
		{
			layoutChildrenForRowDirection(nodesList);
		}/* else if (getDirection().equals(FlexBoxDirection.COLUMN) || getDirection().equals(FlexBoxDirection.COLUMN_REVERSE))
		{
			layoutChildrenForColumnDirection(nodesList);
		}*/

		long duration = System.nanoTime() - timeStart;
		if (verbose) {
			System.out.println(String.format("# layout duration: %d ms", duration / 1000));
		}
	}

	/*private void layoutChildrenForColumnDirection(List<FlexBoxItem> nodesList) {
		double w = getWidth();
		double growWidth = w - getPadding().getLeft() - getPadding().getRight();

		//  double lastX2 = 0;
		int row = 0;
		int i = 0;

		ListIterator<Node> nodeIterator = null;
		if (getDirection().equals(FlexBoxDirection.COLUMN_REVERSE)) {
			nodeIterator = new ReverseListIterator<Node>(getManagedChildren());
		} else {
			nodeIterator = getManagedChildren().listIterator();
		}

		for (FlexBoxItem flexBoxItem : nodesList) {
			FlexBoxRow flexBoxRow = new FlexBoxRow();
			flexBoxRow.addFlexBoxItem(flexBoxItem);
			addToGrid(row, flexBoxRow);
			row++;
			i++;
		}

		if (verbose) {
			System.out.println("grid = " + grid);
		}


		//Rows durchgehen und width berechnen
		double lastY2 = getPadding().getTop();
		i = 0;
		int noGridRows = grid.size();

		//iterate all rows and calculate node sizes and positions
		for (Integer rowIndex : grid.keySet()) {
			//contains all nodes per row
			FlexBoxRow flexBoxRow = grid.get(rowIndex);
			ArrayList<FlexBoxItem> rowNodes = flexBoxRow.getNodes();
			double rowNodeX2 = 0;
			double lastMaxHeight = 0;
			//iterate node of row
			for (FlexBoxItem flexBoxItem : rowNodes) {
				double rowNodeMinWidth = flexBoxItem.node.minWidth(10);
				double rowNodeMaxWidth = flexBoxItem.node.maxWidth(10);
				double rowNodeWidth = Math.min(rowNodeMaxWidth, Math.max(growWidth, rowNodeMinWidth));

				double h = flexBoxItem.node.prefHeight(growWidth);
				flexBoxItem.node.resizeRelocate(rowNodeX2, lastY2, rowNodeWidth, h);

				lastMaxHeight = Math.max(lastMaxHeight, h);

				rowNodeX2 = rowNodeX2 + rowNodeWidth + getHorizontalSpace();
			}

			lastY2 = lastY2 + lastMaxHeight;
			if (i + 1 < noGridRows) {
				lastY2 += getVerticalSpace();
			}
			i++;
		}
	}*/

	private void layoutChildrenForRowDirection(List<FlexBoxItem> nodesList) {
		performingLayout = true;

		double paddings = getPadding().getLeft() + getPadding().getRight();
		double w = getWidth() - paddings;

		nodesList.forEach(flexBoxItem -> {
			flexBoxItem.minWidth = flexBoxItem.node.minWidth(10);
			flexBoxItem.order = getOrder(flexBoxItem.node);
			flexBoxItem.grow = getGrow(flexBoxItem.node);
		});

		if (nodesList.stream().anyMatch(e -> e.order != 0)) {
			nodesList = FXCollections.observableArrayList(nodesList.stream().sorted(Comparator.comparing(f -> f.order)).collect(Collectors.toList()));
		} else {
			nodesList = FXCollections.observableArrayList(nodesList);
		}


		int index = 0;
		int row = 0;
		while(index < nodesList.size()) {
			double currentWidth = 0;
			FlexBoxRow flexBoxRow = new FlexBoxRow();
			addToGrid(row, flexBoxRow);
			while(currentWidth < w && index < nodesList.size()) {
				FlexBoxItem item = nodesList.get(index++);
				if(currentWidth != 0) currentWidth += getHorizontalSpace();
				if(currentWidth == 0 && item.minWidth > w){
					flexBoxRow.addFlexBoxItem(item);
					flexBoxRow.flexGrowSum += item.grow;
					flexBoxRow.rowMinWidth += item.minWidth;
					row++;
					break;
				}
				if(currentWidth + item.minWidth + paddings > w) {
					row++;
					index--;
					break;
				}
				flexBoxRow.addFlexBoxItem(item);
				currentWidth += item.minWidth;
				flexBoxRow.rowMinWidth += item.minWidth;
				flexBoxRow.flexGrowSum += item.grow;
			}
		}

		int noGridRows = grid.size();

		double currentY = getPadding().getTop();
		for(int r = 0; r < noGridRows; r++) {
			FlexBoxRow item = grid.get(r);
			List<FlexBoxItem> items = item.getNodes();
			double remainingWidth = w - (item.getNodes().stream().mapToDouble(n -> n.minWidth + getHorizontalSpace()).sum() - getHorizontalSpace());
			double growSum = item.flexGrowSum;
			double singleGrow = remainingWidth / growSum;
			if(getDirection() == FlexBoxDirection.ROW_REVERSE) items = reverse(items);
			double currentX = getPadding().getLeft();
			double rowHeight = 0;
			for(FlexBoxItem rowItem : items) {
				double maxWidth = rowItem.node.maxWidth(10);
				double additionalWidth = singleGrow * rowItem.grow;
				double width = Math.min(maxWidth, rowItem.minWidth + additionalWidth);
				width = Math.max(width, rowItem.minWidth);
				remainingWidth -= additionalWidth;
				growSum -= rowItem.grow;
				singleGrow = remainingWidth / growSum;
				double height = rowItem.node.prefHeight(width);
				rowHeight = Math.max(height, rowHeight);
				rowItem.node.resizeRelocate(currentX, currentY, width, height);
				currentX += width + getHorizontalSpace();
			}
			currentY += rowHeight + getVerticalSpace();
		}
		currentY += getPadding().getBottom();

		setMinHeight(currentY);
		setPrefHeight(currentY);
		setPrefWidth(currentY);
		computedMinHeight = currentY;
		performingLayout = false;
	}

	private List<FlexBoxItem> reverse(List<FlexBoxItem> items) {
		return IntStream.range(0, items.size()).mapToObj(i -> items.get(items.size() - 1 - i)).collect(Collectors.toList());
	}


	@Override
	public void requestLayout() {
		if (performingLayout) {
			return;
		}
		biasDirty = true;
		bias = null;
		super.requestLayout();

	}

	private void addToGrid(int row, FlexBoxRow flexBoxRow) {
		grid.put(row, flexBoxRow);
	}
}
