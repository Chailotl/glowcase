package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.google.common.primitives.Ints;
import dev.hephaestus.glowcase.block.entity.WireframeBlockEntity;
import dev.hephaestus.glowcase.packet.C2SEditWireframeBlock;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Vec3i;

import java.util.function.Predicate;

public class WireframeBlockEditScreen extends GlowcaseScreen {
	private static final Predicate<String> TEXT_PREDICATE = s -> s.matches("-?\\d*");

	private final WireframeBlockEntity wireframeBlockEntity;

	private TextWidget offsetWidget;
	private TextWidget scaleWidget;
	private TextFieldWidget xOffsetWidget;
	private TextFieldWidget yOffsetWidget;
	private TextFieldWidget zOffsetWidget;
	private TextFieldWidget xScaleWidget;
	private TextFieldWidget yScaleWidget;
	private TextFieldWidget zScaleWidget;
	private TextFieldWidget colorEntryWidget;

	public WireframeBlockEditScreen(WireframeBlockEntity wireframeBlockEntity) {
		this.wireframeBlockEntity = wireframeBlockEntity;
	}

	@Override
	public void init() {
		super.init();

		if (this.client == null) return;

		this.offsetWidget = new TextWidget(width / 2 - 110, height / 2 - 25, 40, 20, Text.translatable("gui.glowcase.offset"), this.textRenderer);
		this.scaleWidget = new TextWidget(width / 2 - 110, height / 2 + 5, 40, 20, Text.translatable("gui.glowcase.scale"), this.textRenderer);

		this.xOffsetWidget = new TextFieldWidget(this.textRenderer, width / 2 - 65, height / 2 - 25, 40, 20, Text.empty());
		this.yOffsetWidget = new TextFieldWidget(this.textRenderer, width / 2 - 20, height / 2 - 25, 40, 20, Text.empty());
		this.zOffsetWidget = new TextFieldWidget(this.textRenderer, width / 2 + 25, height / 2 - 25, 40, 20, Text.empty());

		this.xScaleWidget = new TextFieldWidget(this.textRenderer, width / 2 - 65, height / 2 + 5, 40, 20, Text.empty());
		this.yScaleWidget = new TextFieldWidget(this.textRenderer, width / 2 - 20, height / 2 + 5, 40, 20, Text.empty());
		this.zScaleWidget = new TextFieldWidget(this.textRenderer, width / 2 + 25, height / 2 + 5, 40, 20, Text.empty());

		this.xOffsetWidget.setText(String.valueOf(this.wireframeBlockEntity.offset.getX()));
		this.yOffsetWidget.setText(String.valueOf(this.wireframeBlockEntity.offset.getY()));
		this.zOffsetWidget.setText(String.valueOf(this.wireframeBlockEntity.offset.getZ()));
		this.xScaleWidget.setText(String.valueOf(this.wireframeBlockEntity.scale.getX()));
		this.yScaleWidget.setText(String.valueOf(this.wireframeBlockEntity.scale.getY()));
		this.zScaleWidget.setText(String.valueOf(this.wireframeBlockEntity.scale.getZ()));

		this.xOffsetWidget.setTextPredicate(TEXT_PREDICATE);
		this.yOffsetWidget.setTextPredicate(TEXT_PREDICATE);
		this.zOffsetWidget.setTextPredicate(TEXT_PREDICATE);
		this.xScaleWidget.setTextPredicate(TEXT_PREDICATE);
		this.yScaleWidget.setTextPredicate(TEXT_PREDICATE);
		this.zScaleWidget.setTextPredicate(TEXT_PREDICATE);

		this.xOffsetWidget.setChangedListener(string -> {
			if (Ints.tryParse(string) instanceof Integer x) {
				Vec3i offset = this.wireframeBlockEntity.offset;
				this.wireframeBlockEntity.offset = new Vec3i(x, offset.getY(), offset.getZ());
			}
		});
		this.yOffsetWidget.setChangedListener(string -> {
			if (Ints.tryParse(string) instanceof Integer y) {
				Vec3i offset = this.wireframeBlockEntity.offset;
				this.wireframeBlockEntity.offset = new Vec3i(offset.getX(), y, offset.getZ());
			}
		});
		this.zOffsetWidget.setChangedListener(string -> {
			if (Ints.tryParse(string) instanceof Integer z) {
				Vec3i offset = this.wireframeBlockEntity.offset;
				this.wireframeBlockEntity.offset = new Vec3i(offset.getX(), offset.getY(), z);
			}
		});

		this.xScaleWidget.setChangedListener(string -> {
			if (Ints.tryParse(string) instanceof Integer x) {
				Vec3i scale = this.wireframeBlockEntity.scale;
				this.wireframeBlockEntity.scale = new Vec3i(x, scale.getY(), scale.getZ());
			}
		});
		this.yScaleWidget.setChangedListener(string -> {
			if (Ints.tryParse(string) instanceof Integer y) {
				Vec3i scale = this.wireframeBlockEntity.scale;
				this.wireframeBlockEntity.scale = new Vec3i(scale.getX(), y, scale.getZ());
			}
		});
		this.zScaleWidget.setChangedListener(string -> {
			if (Ints.tryParse(string) instanceof Integer z) {
				Vec3i scale = this.wireframeBlockEntity.scale;
				this.wireframeBlockEntity.scale = new Vec3i(scale.getX(), scale.getY(), z);
			}
		});

		this.xOffsetWidget.setPlaceholder(Text.translatable("gui.glowcase.x_offset"));
		this.yOffsetWidget.setPlaceholder(Text.translatable("gui.glowcase.y_offset"));
		this.zOffsetWidget.setPlaceholder(Text.translatable("gui.glowcase.z_offset"));
		this.xScaleWidget.setPlaceholder(Text.translatable("gui.glowcase.x_scale"));
		this.yScaleWidget.setPlaceholder(Text.translatable("gui.glowcase.y_scale"));
		this.zScaleWidget.setPlaceholder(Text.translatable("gui.glowcase.z_scale"));

		this.colorEntryWidget = new TextFieldWidget(this.client.textRenderer, width / 2 - 25, height / 2 + 35, 50, 20, Text.empty());
		this.colorEntryWidget.setText("#" + String.format("%1$06X", this.wireframeBlockEntity.color & 0x00FFFFFF));
		this.colorEntryWidget.setChangedListener(string -> {
			TextColor.parse(this.colorEntryWidget.getText()).ifSuccess(color -> {
				this.wireframeBlockEntity.color = color == null ? 0xFFFFFFFF : color.getRgb() | 0xFF000000;
			});
		});

		this.addDrawableChild(this.offsetWidget);
		this.addDrawableChild(this.scaleWidget);
		this.addDrawableChild(this.xOffsetWidget);
		this.addDrawableChild(this.yOffsetWidget);
		this.addDrawableChild(this.zOffsetWidget);
		this.addDrawableChild(this.xScaleWidget);
		this.addDrawableChild(this.yScaleWidget);
		this.addDrawableChild(this.zScaleWidget);
		this.addDrawableChild(this.colorEntryWidget);
	}

		@Override
	public void close() {
		C2SEditWireframeBlock.of(wireframeBlockEntity).send();
		super.close();
	}
}
