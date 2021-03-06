/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:JvmName("EditorHelperRt")

package com.maddyhome.idea.vim.helper

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.maddyhome.idea.vim.option.OptionsManager
import kotlin.system.measureTimeMillis

val Editor.fileSize: Int
  get() = document.textLength

/**
 * There is a problem with one-line editors. At the moment of the editor creation, this property is always set to false.
 *   So, we should enable IdeaVim for such editors and disable it on the first interaction
 */
val Editor.isIdeaVimDisabledHere: Boolean
  get() {
    var res = true
    val timeForCalculation = measureTimeMillis {
      res = (disabledInDialog
        || isDatabaseCell && !OptionsManager.oneline.isSet
        || isOneLineMode && !OptionsManager.oneline.isSet)
    }
    if (timeForCalculation > 10) {
      logger<Editor>().error("Time for calculation of 'isIdeaVimDisabledHere' took $timeForCalculation ms.")
    }
    return res
  }

private val Editor.isDatabaseCell: Boolean
  get() = DarculaUIUtil.isTableCellEditor(this.component)

private val Editor.disabledInDialog: Boolean
  get() = OptionsManager.dialogescape.value == "off" && (!this.isPrimaryEditor() && !EditorHelper.isFileEditor(this))

/**
 * Checks if the editor is a primary editor in the main editing area.
 */
fun Editor.isPrimaryEditor(): Boolean {
  val project = project ?: return false
  val fileEditorManager = FileEditorManagerEx.getInstanceEx(project) ?: return false
  return fileEditorManager.allEditors.any { fileEditor -> this == EditorUtil.getEditorEx(fileEditor) }
}

val Caret.amountOfInlaysBeforeCaret: Int
  get() {
    val curLineStartOffset: Int = this.editor.document.getLineStartOffset(logicalPosition.line)
    return this.editor.inlayModel.getInlineElementsInRange(curLineStartOffset, this.offset).size
  }

fun Editor.amountOfInlaysBeforeVisualPosition(pos: VisualPosition): Int {
  val newOffset = EditorHelper.visualPositionToOffset(this, pos)
  val lineStartNewOffset: Int = this.document.getLineStartOffset(this.visualToLogicalPosition(pos).line)
  return this.inlayModel.getInlineElementsInRange(lineStartNewOffset, newOffset).size
}
