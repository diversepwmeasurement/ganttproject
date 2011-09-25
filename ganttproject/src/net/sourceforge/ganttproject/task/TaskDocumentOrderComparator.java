/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.task;

import java.util.Comparator;

class TaskDocumentOrderComparator implements Comparator<Task> {
    private final TaskManagerImpl myManager;
    TaskDocumentOrderComparator(TaskManagerImpl taskManager) {
        myManager = taskManager;
    }
    public int compare(Task task1, Task tasl2) {
        // TODO assert can be removed since it is checked by Java compiler?
        assert (task1 instanceof Task && tasl2 instanceof Task): "I compare only tasks";
        return myManager.getTaskHierarchy().compareDocumentOrder(task1, tasl2);
    }
}
