package com.github.badfalcon.backlog.util

import com.intellij.ui.table.JBTable
import javax.swing.table.TableCellRenderer

object TableUtils {
    fun autoResizeTableColumns(table: JBTable) {
        val header = table.tableHeader
        val columnModel = table.columnModel
        for (column in 0 until columnModel.columnCount) {
            var maxWidth = header.defaultRenderer
                .getTableCellRendererComponent(
                    table,
                    header.columnModel.getColumn(column).headerValue,
                    false,
                    false,
                    -1,
                    column
                )
                .preferredSize.width
            for (row in 0 until table.rowCount) {
                val cellRenderer: TableCellRenderer = table.getCellRenderer(row, column)
                val cellComponent = table.prepareRenderer(cellRenderer, row, column)
                val cellWidth = cellComponent.preferredSize.width
                maxWidth = maxOf(maxWidth, cellWidth)
            }
            columnModel.getColumn(column).preferredWidth = maxWidth + 20
        }
    }
}
