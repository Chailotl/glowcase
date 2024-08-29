package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditPopupBlock;
import dev.hephaestus.glowcase.packet.C2SEditTextBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

//TODO: multi-character selection at some point? it may be a bit complex but it'd be nice
public class PopupBlockEditScreen extends GlowcaseScreen {
	private final PopupBlockEntity popupBlockEntity;

	private SelectionManager selectionManager;
	private int currentRow;
	private long ticksSinceOpened = 0;
	private ButtonWidget changeAlignment;
	private TextFieldWidget colorEntryWidget;

	public PopupBlockEditScreen(PopupBlockEntity popupBlockEntity) {
		this.popupBlockEntity = popupBlockEntity;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		renderDarkening(context);
	}

	@Override
	public void init() {
		super.init();

		int innerPadding = width / 100;

		this.selectionManager = new SelectionManager(
			() -> this.popupBlockEntity.getRawLine(this.currentRow),
			(string) -> {
				popupBlockEntity.setRawLine(this.currentRow, string);
				this.popupBlockEntity.renderDirty = true;
			},
			SelectionManager.makeClipboardGetter(this.client),
			SelectionManager.makeClipboardSetter(this.client),
			(string) -> true);

		this.changeAlignment = ButtonWidget.builder(Text.stringifiedTranslatable("gui.glowcase.alignment", this.popupBlockEntity.textAlignment), action -> {
			switch (popupBlockEntity.textAlignment) {
				case LEFT -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.CENTER;
				case CENTER -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.RIGHT;
				case RIGHT -> popupBlockEntity.textAlignment = TextBlockEntity.TextAlignment.LEFT;
			}
			this.popupBlockEntity.renderDirty = true;

			this.changeAlignment.setMessage(Text.stringifiedTranslatable("gui.glowcase.alignment", this.popupBlockEntity.textAlignment));
		}).dimensions(120 + innerPadding, 0, 160, 20).build();

		this.colorEntryWidget = new TextFieldWidget(this.client.textRenderer, 280 + innerPadding * 2, 0, 50, 20, Text.empty());
		this.colorEntryWidget.setText("#" + Integer.toHexString(this.popupBlockEntity.color & 0x00FFFFFF));
		this.colorEntryWidget.setChangedListener(string -> {
			TextColor.parse(this.colorEntryWidget.getText()).ifSuccess(color -> {
				this.popupBlockEntity.color = color == null ? 0xFFFFFFFF : color.getRgb() | 0xFF000000;
				this.popupBlockEntity.renderDirty = true;
			});
		});

		this.addDrawableChild(this.changeAlignment);
		this.addDrawableChild(this.colorEntryWidget);
	}

	@Override
	public void tick() {
		++this.ticksSinceOpened;
	}

	@Override
	public void close() {
		C2SEditPopupBlock.of(popupBlockEntity).send();
		super.close();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (this.client != null) {
			super.render(context, mouseX, mouseY, delta);

			context.getMatrices().push();
			context.getMatrices().translate(0, 40 + 2 * this.width / 100F, 0);
			for (int i = 0; i < this.popupBlockEntity.lines.size(); ++i) {
				var text = this.currentRow == i ? Text.literal(this.popupBlockEntity.getRawLine(i)) : this.popupBlockEntity.lines.get(i);

				int lineWidth = this.textRenderer.getWidth(text);
				switch (this.popupBlockEntity.textAlignment) {
					case LEFT -> context.drawTextWithShadow(client.textRenderer, text, this.width / 10, i * 12, this.popupBlockEntity.color);
					case CENTER -> context.drawTextWithShadow(client.textRenderer, text, this.width / 2 - lineWidth / 2, i * 12, this.popupBlockEntity.color);
					case RIGHT -> context.drawTextWithShadow(client.textRenderer, text, this.width - this.width / 10 - lineWidth, i * 12, this.popupBlockEntity.color);
				}
			}

			int caretStart = this.selectionManager.getSelectionStart();
			int caretEnd = this.selectionManager.getSelectionEnd();

			if (caretStart >= 0) {
				String line = this.popupBlockEntity.getRawLine(this.currentRow);
				int selectionStart = MathHelper.clamp(Math.min(caretStart, caretEnd), 0, line.length());
				int selectionEnd = MathHelper.clamp(Math.max(caretStart, caretEnd), 0, line.length());

				String preSelection = line.substring(0, MathHelper.clamp(line.length(), 0, selectionStart));
				int startX = this.client.textRenderer.getWidth(preSelection);

				float push = switch (this.popupBlockEntity.textAlignment) {
					case LEFT -> this.width / 10F;
					case CENTER -> this.width / 2F - this.textRenderer.getWidth(line) / 2F;
					case RIGHT -> this.width - this.width / 10F - this.textRenderer.getWidth(line);
				};

				startX += (int) push;


				int caretStartY = this.currentRow * 12;
				int caretEndY = this.currentRow * 12 + 9;
				if (this.ticksSinceOpened / 6 % 2 == 0 && !this.colorEntryWidget.isActive()) {
					if (selectionStart < line.length()) {
						context.fill(startX, caretStartY, startX + 1, caretEndY, 0xCCFFFFFF);
					} else {
						context.drawText(client.textRenderer, "_", startX, this.currentRow * 12, 0xFFFFFFFF, false);
					}
				}

				if (caretStart != caretEnd) {
					int endX = startX + this.client.textRenderer.getWidth(line.substring(selectionStart, selectionEnd));
					Tessellator tessellator = Tessellator.getInstance();
					BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
					RenderSystem.enableColorLogicOp();
					RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
					bufferBuilder.vertex(context.getMatrices().peek().getPositionMatrix(), startX, caretEndY, 0.0F).color(0, 0, 255, 255);
					bufferBuilder.vertex(context.getMatrices().peek().getPositionMatrix(), endX, caretEndY, 0.0F).color(0, 0, 255, 255);
					bufferBuilder.vertex(context.getMatrices().peek().getPositionMatrix(), endX, caretStartY, 0.0F).color(0, 0, 255, 255);
					bufferBuilder.vertex(context.getMatrices().peek().getPositionMatrix(), startX, caretStartY, 0.0F).color(0, 0, 255, 255);
					BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
					RenderSystem.disableColorLogicOp();
				}
			}

			context.getMatrices().pop();
		}
	}

	@Override
	public boolean charTyped(char chr, int keyCode) {
		if (this.colorEntryWidget.isActive()) {
			return this.colorEntryWidget.charTyped(chr, keyCode);
		} else {
			this.selectionManager.insert(chr);
			return true;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.colorEntryWidget.isActive()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				this.close();
				return true;
			} else {
				return this.colorEntryWidget.keyPressed(keyCode, scanCode, modifiers);
			}
		} else {
			setFocused(null);
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				this.popupBlockEntity.addRawLine(this.currentRow + 1,
					this.popupBlockEntity.getRawLine(this.currentRow).substring(
						MathHelper.clamp(this.selectionManager.getSelectionStart(), 0, this.popupBlockEntity.getRawLine(this.currentRow).length())
					));
				this.popupBlockEntity.setRawLine(this.currentRow,
					this.popupBlockEntity.getRawLine(this.currentRow).substring(0, MathHelper.clamp(this.selectionManager.getSelectionStart(), 0, this.popupBlockEntity.getRawLine(this.currentRow).length())
					));
				this.popupBlockEntity.renderDirty = true;
				++this.currentRow;
				this.selectionManager.moveCursorToStart();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_UP) {
				this.currentRow = Math.max(this.currentRow - 1, 0);
				this.selectionManager.putCursorAtEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
				this.currentRow = Math.min(this.currentRow + 1, (this.popupBlockEntity.lines.size() - 1));
				this.selectionManager.putCursorAtEnd();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.currentRow > 0 && this.popupBlockEntity.lines.size() > 1 && this.selectionManager.getSelectionStart() == 0 && this.selectionManager.getSelectionEnd() == this.selectionManager.getSelectionStart()) {
				--this.currentRow;
				this.selectionManager.putCursorAtEnd();
				deleteLine();
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DELETE && this.currentRow < this.popupBlockEntity.lines.size() - 1 && this.selectionManager.getSelectionEnd() == this.popupBlockEntity.getRawLine(this.currentRow).length()) {
				deleteLine();
				return true;
			} else {
				try {
					boolean val = this.selectionManager.handleSpecialKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
					int selectionOffset = this.popupBlockEntity.getRawLine(this.currentRow).length() - this.selectionManager.getSelectionStart();

					// Find line feed characters and create proper newlines
					for (int i = 0; i < this.popupBlockEntity.lines.size(); ++i) {
						int lineFeedIndex = this.popupBlockEntity.getRawLine(i).indexOf("\n");

						if (lineFeedIndex >= 0) {
							this.popupBlockEntity.addRawLine(i + 1,
								this.popupBlockEntity.getRawLine(i).substring(
									MathHelper.clamp(lineFeedIndex + 1, 0, this.popupBlockEntity.getRawLine(i).length())
								));
							this.popupBlockEntity.setRawLine(i,
								this.popupBlockEntity.getRawLine(i).substring(0, MathHelper.clamp(lineFeedIndex, 0, this.popupBlockEntity.getRawLine(i).length())
								));
							this.popupBlockEntity.renderDirty = true;
							++this.currentRow;
							this.selectionManager.putCursorAtEnd();
							this.selectionManager.moveCursor(-selectionOffset);
						}
					}
					return val;
				} catch (StringIndexOutOfBoundsException e) {
					e.printStackTrace();
					MinecraftClient.getInstance().setScreen(null);
					return false;
				}
			}
		}
	}

	private void deleteLine() {
		this.popupBlockEntity.setRawLine(this.currentRow,
			this.popupBlockEntity.getRawLine(this.currentRow) + this.popupBlockEntity.getRawLine(this.currentRow + 1)
		);

		this.popupBlockEntity.lines.remove(this.currentRow + 1);
		this.popupBlockEntity.renderDirty = true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int topOffset = (int) (40 + 2 * this.width / 100F);
		if (!this.colorEntryWidget.mouseClicked(mouseX, mouseY, button)) {
			this.colorEntryWidget.setFocused(false);
		}
		if (mouseY > topOffset) {
			this.currentRow = MathHelper.clamp((int) (mouseY - topOffset) / 12, 0, this.popupBlockEntity.lines.size() - 1);
			this.setFocused(null);
			String baseContents = this.popupBlockEntity.getRawLine(currentRow);
			int baseContentsWidth = this.textRenderer.getWidth(baseContents);
			int contentsStart;
			int contentsEnd;
			switch (this.popupBlockEntity.textAlignment) {
				case LEFT -> {
					contentsStart = this.width / 10;
					contentsEnd = contentsStart + baseContentsWidth;
				}
				case CENTER -> {
					int midpoint = this.width / 2;
					int textMidpoint = baseContentsWidth / 2;
					contentsStart = midpoint - textMidpoint;
					contentsEnd = midpoint + textMidpoint;
				}
				case RIGHT -> {
					contentsEnd = this.width - this.width / 10;
					contentsStart = contentsEnd - baseContentsWidth;
				}
				//even though this is exhaustive, javac won't treat contentsStart and contentsEnd as initialized
				//why? who knows! just throw bc this should be impossible
				default -> throw new IllegalStateException(":HOW:");
			}

			if (mouseX <= contentsStart) {
				this.selectionManager.moveCursorToStart();
			} else if (mouseX >= contentsEnd) {
				this.selectionManager.putCursorAtEnd();
			} else {
				int lastWidth = 0;
				for (int i = 1; i < baseContents.length(); i++) {
					String testContents = baseContents.substring(0, i);
					int width = this.textRenderer.getWidth(testContents);
					int midpointWidth = (width + lastWidth) / 2;
					if (mouseX < contentsStart + midpointWidth) {
						this.selectionManager.moveCursorTo(i - 1, false);
						break;
					} else if (mouseX <= contentsStart + width) {
						this.selectionManager.moveCursorTo(i, false);
						break;
					}
					lastWidth = width;
				}
			}
			return true;
		} else {
			return super.mouseClicked(mouseX, mouseY, button);
		}
	}
}
