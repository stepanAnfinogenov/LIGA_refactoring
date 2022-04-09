package framework.ru.documentum.utils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;

import com.documentum.services.collaboration.ICalendarEvent;
import com.documentum.services.collaboration.IRecurrenceSet;
import com.documentum.services.richtext.IRichText;

import framework.ru.documentum.utils.RecurrenceRule;
import framework.ru.documentum.utils.IQueryProcessor;
import framework.ru.documentum.utils.QueryHelper;
import framework.ru.documentum.utils.QueryUtils;
import framework.ru.documentum.services.DsHelper;

/**
 * Работа с календарем. <br>
 * Замена IWorkflowCalendar.
 * 
 * 1) Сервис не работает для отрицательных сдвигов, как и IWorkflowCalendar.
 * 
 * 
 */
public class CalendarExHelper extends DsHelper {
	public static String WORKING_DAY_KEYWORD = "Working Day";
    public static String NON_WORKING_DAY_KEYWRORD = "Non-Working Day";

    private boolean enableTrace = false;

    public CalendarExHelper(IDfSession session) throws DfException {
	super(session);
    }

    public IDfId getCalendar(String name) throws DfException {
	String query = "dmc_calendar where object_name=''{0}''";
	query = MessageFormat.format(query, QueryUtils.makeStringLiteral(name));
	IDfSysObject sys = (IDfSysObject) session.getObjectByQualification(query);
	if (sys == null) {
	    throw new DfException("Calendar not found: " + name);
	}
	return sys.getObjectId();
    }

    /**
     * Для computeOffSetTime нужны только несколько полей.
     * 
     * @author vereta
     * 
     */
    private class CalendarEvent implements ICalendarEvent {

	private Date start, end;

	public CalendarEvent(Date start, Date end) {
	    super();
	    this.start = start;
	    this.end = end;
	}

	@Override
	public String getObjectName() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public IDfId getObjectId() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public IRichText getDescription() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public IDfTime getStartDate() throws DfException {
	    return new DfTime(start);
	}

	@Override
	public IDfTime getEndDate() throws DfException {
	    return new DfTime(end);
	}

	@Override
	public boolean getDateOnly() throws DfException {
	    throw new DfException("Not implemented");
	}

	/**
	 * Возвращает длину интервала в секундах.
	 */
	@Override
	public int getDuration() throws DfException {
	    return (int) ((end.getTime() - start.getTime()) / 1000);
	}

	@Override
	public String getLocation() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public String getOrganizer() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public int getPriority() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public String getUniqueId() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public boolean isTransparent() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public boolean isRecurringEvent() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public IRecurrenceSet getRecurrenceSet() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public IDfTime getRecurrenceId() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public String[] getAttendees() throws DfException {
	    throw new DfException("Not implemented");
	}

	@Override
	public IDfTypedObject getEventData() throws DfException {
	    throw new DfException("Not implemented");
	}

    }

    /**
     * Возвращает список рабочих дней.
     * 
     * @param calendarId
     *            ID календаря.
     * @param start
     *            Текущая дата.
     * @return
     * @throws DfException
     */
    public List<ICalendarEvent> getWorkingDays(IDfId calendarId, Date start) throws DfException {
	return getDays(calendarId, start, WORKING_DAY_KEYWORD, true);
    }

    /**
     * Возвращает список не рабочих дней.
     * 
     * @param calendarId
     *            ID календаря.
     * @param start
     *            Текущая дата.
     * @return
     * @throws DfException
     */
    public List<ICalendarEvent> getNonWorkingDays(IDfId calendarId, Date start) throws DfException {
	return getDays(calendarId, start, NON_WORKING_DAY_KEYWRORD, true);
    }

    /**
     * Используется для getDays.
     */
    private List<ICalendarEvent> list;

    /**
     * Используется для getDays.
     */
    private List<ICalendarEvent> excludeList;

    /**
     * Добавляет в список событий новый элемент.
     * 
     * @param startDate
     *            Начальная дата.
     * @param endDate
     *            Конечная дата.
     */
    private void add(Date startDate, Date endDate) {
	if (startDate.equals(endDate) == false) {
	    list.add(new CalendarEvent(startDate, endDate));
	}
    }

    /**
     * Инициализация списка дней-исключений. Эти временные промежутки будут
     * исключены из результатов вызова getDays.
     * 
     * @param calendarId
     * @param start
     * @param keyword
     * @throws DfException
     */
    private void initExcludeList(IDfId calendarId, Date start, String keyword) throws DfException {
	if (keyword.equals(WORKING_DAY_KEYWORD)) {
	    // Будем вычитать из рабочих дней отдельно заданные нерабочие дни.
	    excludeList = new CalendarExHelper(session).getDays(calendarId, start, NON_WORKING_DAY_KEYWRORD, false);
	} else {
	    excludeList = new ArrayList<ICalendarEvent>();
	}
    }

    /**
     * Проверка, не содержит ли заданный промежуток промежутков-исключений.
     * 
     * @param current
     * @return
     */
    private boolean filterByExcludeList(CalendarEvent current) {
	boolean needAdd = true;
	for (ICalendarEvent item : excludeList) {

	    CalendarEvent excludeItem = (CalendarEvent) item;

	    boolean itemProcessed = true;
	    if ((excludeItem.start.compareTo(current.start) >= 0) && (excludeItem.end.compareTo(current.end) <= 0)) {
		// Промежуток-исключение посередине текущего промежутка.

		add(current.start, excludeItem.start);
		add(excludeItem.end, current.end);
	    } else if ((excludeItem.start.compareTo(current.start) <= 0)
		    && (excludeItem.end.compareTo(current.end) >= 0)) {
		// Текущий промежуток посередине промежутка-исключения.

		// Not add, day excluded
	    } else if ((excludeItem.start.compareTo(current.start) >= 0)
		    && (excludeItem.start.compareTo(current.end) <= 0)) {
		// Начало промежутка-исключения посередине текущего промежутка.

		add(current.start, excludeItem.start);
	    } else if ((excludeItem.end.compareTo(current.start) >= 0) && (excludeItem.end.compareTo(current.end) <= 0)) {
		// Конец промежутка-исключения посередине текущего промежутка.

		add(excludeItem.end, current.end);
	    } else {
		// Промежуток-исключение не пересекатся с текущим промежутком.

		itemProcessed = false;
	    }

	    if (itemProcessed) {
		debug("Exclude {0}, {1} by {2}, {3}", current.start, current.end, excludeItem.start, excludeItem.end);

		needAdd = false;
		break;
	    }
	}

	return needAdd;
    }

    /**
     * Загрузка списка временных промежутков данного календаря.
     * 
     * @param calendarId
     *            ID календаря.
     * @param start
     *            Текущая дата (используется только чтобы ограничить загружаемые
     *            времена сверху).
     * @param keyword
     *            Тип промежутка.
     * @throws DfException
     */
    private void loadDaysList(IDfId calendarId, Date start, String keyword) throws DfException {
	list = new ArrayList<ICalendarEvent>();

	Calendar calendar = Calendar.getInstance();
	calendar.setTime(start);
	calendar.add(Calendar.YEAR, 2);
	final Date max = calendar.getTime();

	String query = "select distinct start_date,end_date,r_object_id,recurrence_end,recurrence_rule,exception_dates,recurrence_dates from dmc_calendar_event where folder(id(''{0}'')) and (ANY keywords=''{1}'') order by r_object_id, start_date";
	query = MessageFormat.format(query, QueryUtils.makeStringLiteral(calendarId.toString()),
		QueryUtils.makeStringLiteral(keyword));
	debug("Query {0}", query);
	new QueryHelper(session).perform(query, new IQueryProcessor() {

	    @Override
	    public boolean process(IDfTypedObject obj) throws DfException {
		IDfTime startDate = obj.getTime("start_date");
		IDfTime endDate = obj.getTime("end_date");
		IDfTime recurrenceEndDate = obj.getTime("recurrence_end");
		String recurrenceRule = obj.getString("recurrence_rule");
		if (recurrenceRule.trim().length() == 0) {
		    if (recurrenceEndDate.isNullDate()) {
			CalendarEvent event = new CalendarEvent(startDate.getDate(), endDate.getDate());
			list.add(event);
		    } else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(recurrenceEndDate.getDate());
			calendar.add(Calendar.HOUR_OF_DAY, 24);
			CalendarEvent event = new CalendarEvent(recurrenceEndDate.getDate(), calendar.getTime());
			list.add(event);
		    }
		} else {

		    RecurrenceRule rule = new RecurrenceRule(recurrenceRule);
		    RecurrenceRule.Iterator it = rule.iterator(startDate);
		    while (true) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(it.getInstance().getDate());
			calendar.add(Calendar.MILLISECOND, (int) (endDate.getDate().getTime() - startDate.getDate()
				.getTime()));
			Date instanceDate = it.getInstance().getDate();
			CalendarEvent event = new CalendarEvent(instanceDate, calendar.getTime());
			list.add(event);
			if (it.next() == false) {
			    break;
			}
			// next может быть до бесконечности, если в правиле не
			// задана конечная дата (теоретически).
			if (max.compareTo(instanceDate) < 0) {
			    break;
			}
		    }
		}

		return true;
	    }
	});

	debug("Found days {0} for {1}", list.size(), keyword);
    }

    /**
     * Возвращает список дней по заданному типу дня.
     * 
     * @param calendarId
     *            ID календаря.
     * @param start
     *            Текущая дата.
     * @param keyword
     *            Тип дня.
     * @param filterByStart
     *            true - возвращаются только объекты с начальной датой >= start.
     * @return
     * @throws DfException
     */
    public List<ICalendarEvent> getDays(IDfId calendarId, Date start, String keyword, boolean filterByStart)
	    throws DfException {

	initExcludeList(calendarId, start, keyword);
	loadDaysList(calendarId, start, keyword);
	if (filterByStart == false) {
	    start = null;
	}

	final List<ICalendarEvent> list2 = new ArrayList<ICalendarEvent>();

	int i = 0;
	while (i < list.size()) {
	    ICalendarEvent event = list.get(i);
	    i++;

	    CalendarEvent current = (CalendarEvent) event;

	    boolean needAdd = true;
	    if (start != null) {

		// Начало дня меньше старта. Но конец больше старта.
		if ((current.start.compareTo(start) < 0) && (current.end.compareTo(start) > 0)) {
		    current.start = start;
		}

		if (current.start.compareTo(start) < 0) {
		    needAdd = false;
		}
	    }

	    if (needAdd) {
		needAdd = filterByExcludeList(current);
	    }

	    if (needAdd) {
		list2.add(event);
	    }
	}

	ICalendarEvent[] items = list2.toArray(new ICalendarEvent[] {});
	Arrays.sort(items, new Comparator<ICalendarEvent>() {

	    @Override
	    public int compare(ICalendarEvent o1, ICalendarEvent o2) {
		try {
		    return o1.getStartDate().getDate().compareTo(o2.getStartDate().getDate());
		} catch (Throwable tr) {
		    throw new RuntimeException(tr);
		}
	    }
	});

	for (ICalendarEvent event : items) {
	    trace("{0}, {1}, {2}", event.getStartDate(), event.getEndDate(), event.getDuration());
	}

	return Arrays.asList(items);
    }

    /**
     * Возвращает время, которое сдвинуто относительно заданного начального
     * времени на заданное число секунд. Использует календарь. <br>
     * При отсутствии в календаре подходящих рабочих дней возвращает заданное
     * начальное время.
     * 
     * @param calendarId
     *            ID календаря.
     * @param startDate
     *            Начальное время.
     * @param requiredDuration
     *            Длина промежутка в секундах.
     * @return
     * @throws DfException
     */
    public IDfTime computeOffSetTime(IDfId calendarId, IDfTime startDate, long requiredDuration, String fixedTime)
	    throws DfException {

	if (requiredDuration == 0L)
	    return startDate;
	if (requiredDuration < 0L) {
	    throw new DfException("INVALID_REQUIRED_DURATION");
	}

	List<ICalendarEvent> events = getWorkingDays(calendarId, startDate.getDate());

	debug("Working days list obtained, {0}", events.size());

	long coveredDuration = 0L;

	IDfTime tempEndDate = startDate;

	for (int i = 0; i < events.size(); i++) {
	    ICalendarEvent event = events.get(i);

	    IDfTime eventStartDate = event.getStartDate();
	    IDfTime eventEndDate = event.getEndDate();
	    long eventDuration = event.getDuration();

	    trace("Process event {0}, {1}, {2} ({3})", eventStartDate, eventEndDate, eventDuration, eventDuration
		    / (24L * 60L * 60L));

	    if (eventStartDate.compareTo(tempEndDate) == 1) {
		tempEndDate = eventStartDate;
	    } else if (eventStartDate.compareTo(tempEndDate) == -1) {
		int compare = eventEndDate.compareTo(tempEndDate);
		if (compare == 0)
		    continue;
		if (compare == -1) {
		    continue;
		}

		eventDuration = (eventEndDate.getDate().getTime() - tempEndDate.getDate().getTime()) / 1000L;
	    }
	    if (coveredDuration + eventDuration == requiredDuration) {
		return eventEndDate;
	    }
	    if (coveredDuration + eventDuration > requiredDuration) {
		long tempEndDateInSecs = tempEndDate.getDate().getTime() / 1000L;
		Date dueDate = new Date((tempEndDateInSecs + requiredDuration - coveredDuration) * 1000L);

		if (fixedTime != null) {
		    fixedTime = fixedTime.trim();

		    if ((fixedTime.length() > 0) && (fixedTime.equalsIgnoreCase("none") == false)) {
			int p = fixedTime.indexOf(":");
			int hour = Integer.parseInt(fixedTime.substring(0, p));
			int minute = Integer.parseInt(fixedTime.substring(p + 1));
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dueDate);
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minute);
			int j = i + 1;
			if ((calendar.getTime().compareTo(dueDate) < 0) && (j < events.size())) {
			    // Next day with specified HH:MM;
			    event = events.get(j);
			    Date startEvent = event.getStartDate().getDate();
			    calendar.setTime(startEvent);
			    calendar.set(Calendar.HOUR_OF_DAY, hour);
			    calendar.set(Calendar.MINUTE, minute);
			    if (calendar.getTime().compareTo(dueDate) > 0) {
				debug("Next day shift {0}->{1} (by {2}}", dueDate, calendar.getTime(), fixedTime);
				dueDate = calendar.getTime();
			    }
			}
		    }
		}

		return new DfTime(dueDate);
	    }

	    tempEndDate = eventEndDate;
	    coveredDuration += eventDuration;
	}

	return startDate;
    }

    protected void trace(String message, Object... params) {
	if (enableTrace) {
	    debug(message, params);
	}
    }

    public class CalendarDate {
	public Date value;
	public int flags;
    };

    public List<CalendarDate> getNonStandardDates(IDfId calendarId, Date start, String keyword, boolean filterByStart)
	    throws DfException {
	// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

	List<CalendarDate> result = new ArrayList<CalendarDate>();
	List<ICalendarEvent> events = getDays(calendarId, start, keyword, filterByStart);
	int flags;

	for (ICalendarEvent event : events) {
	    Calendar calendar = Calendar.getInstance();
	    Date eventStart = event.getStartDate().getDate();
	    calendar.setTime(eventStart);
	    while (true) {
		CalendarDate item = new CalendarDate();
		if (keyword.equals(WORKING_DAY_KEYWORD)) {
		    flags = 1;
		} else {
		    flags = -1;
		}

		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		boolean exclude;
		if (keyword.equals(WORKING_DAY_KEYWORD)) {
		    // Обычный рабочий день исключаем. Включаем только рабочие
		    // дни в
		    // выходные.
		    exclude = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
		} else {
		    // Праздник, выпадает на выходной - исключаем.
		    exclude = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
		}

		item.value = eventStart;
		item.flags = flags;

		if (exclude == false) {
		    result.add(item);
		}
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		if (calendar.getTime().compareTo(event.getEndDate().getDate()) >= 0) {
		    break;
		}
	    }
	}

	return result;
    }

    /**
     * Возвращает список нестандартных дней. Праздники, выпадающие на будние дни
     * и рабочие дни, выпадающие на выходные.
     * 
     * @param calendarId
     *            ID календаря.
     * @param start
     *            Дата начала, можно не указывать.
     * @param filterByStart
     *            false - дата начала не используется, даже если задана.
     * @return
     * @throws Exception
     */
    public List<CalendarDate> getNonStandardDates(IDfId calendarId, Date start, boolean filterByStart) throws Exception {
	final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	if (start == null) {
	    start = format.parse("2000-01-01");
	}
	List<CalendarDate> result = new ArrayList<CalendarDate>();
	result.addAll(getNonStandardDates(calendarId, start, WORKING_DAY_KEYWORD, filterByStart));
	result.addAll(getNonStandardDates(calendarId, start, NON_WORKING_DAY_KEYWRORD, filterByStart));

	Collections.sort(result, new Comparator<CalendarDate>() {

	    @Override
	    public int compare(CalendarDate o1, CalendarDate o2) {
		String str1 = format.format(o1.value);
		String str2 = format.format(o2.value);
		return str1.compareTo(str2);
	    }
	});

	return result;
    }
}
