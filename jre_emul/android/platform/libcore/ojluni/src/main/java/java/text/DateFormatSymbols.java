/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import libcore.icu.ICU;
import libcore.icu.LocaleData;
import libcore.icu.TimeZoneNames;

// Android-removed: Remove javadoc related to "rg" Locale extension.
// The "rg" extension isn't supported until https://unicode-org.atlassian.net/browse/ICU-21831
// is resolved, because java.text.* stack relies on ICU on resource resolution.
/**
 * <code>DateFormatSymbols</code> is a public class for encapsulating
 * localizable date-time formatting data, such as the names of the
 * months, the names of the days of the week, and the time zone data.
 * <code>SimpleDateFormat</code> uses
 * <code>DateFormatSymbols</code> to encapsulate this information.
 *
 * <p>
 * Typically you shouldn't use <code>DateFormatSymbols</code> directly.
 * Rather, you are encouraged to create a date-time formatter with the
 * <code>DateFormat</code> class's factory methods: <code>getTimeInstance</code>,
 * <code>getDateInstance</code>, or <code>getDateTimeInstance</code>.
 * These methods automatically create a <code>DateFormatSymbols</code> for
 * the formatter so that you don't have to. After the
 * formatter is created, you may modify its format pattern using the
 * <code>setPattern</code> method. For more information about
 * creating formatters using <code>DateFormat</code>'s factory methods,
 * see {@link DateFormat}.
 *
 * <p>
 * If you decide to create a date-time formatter with a specific
 * format pattern for a specific locale, you can do so with:
 * <blockquote>
 * <pre>
 * new SimpleDateFormat(aPattern, DateFormatSymbols.getInstance(aLocale)).
 * </pre>
 * </blockquote>
 *
 * <p>
 * <code>DateFormatSymbols</code> objects are cloneable. When you obtain
 * a <code>DateFormatSymbols</code> object, feel free to modify the
 * date-time formatting data. For instance, you can replace the localized
 * date-time format pattern characters with the ones that you feel easy
 * to remember. Or you can change the representative cities
 * to your favorite ones.
 *
 * <p>
 * New <code>DateFormatSymbols</code> subclasses may be added to support
 * <code>SimpleDateFormat</code> for date-time formatting for additional locales.

 * @see          DateFormat
 * @see          SimpleDateFormat
 * @see          java.util.SimpleTimeZone
 * @author       Chen-Lieh Huang
 * @since 1.1
 */
public class DateFormatSymbols implements Serializable, Cloneable {

    // Android-changed: Removed reference to DateFormatSymbolsProvider but suggested getInstance().
    // be used instead in case Android supports it in future.
    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the default {@link java.util.Locale.Category#FORMAT FORMAT}
     * locale. It is recommended that the {@link #getInstance(Locale) getInstance} method is used
     * instead.
     * <p>This is equivalent to calling
     * {@link #DateFormatSymbols(Locale)
     *     DateFormatSymbols(Locale.getDefault(Locale.Category.FORMAT))}.
     * @see #getInstance()
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @exception  java.util.MissingResourceException
     *             if the resources for the default locale cannot be
     *             found or cannot be loaded.
     */
    public DateFormatSymbols()
    {
        initializeData(Locale.getDefault(Locale.Category.FORMAT));
    }

    // Android-changed: Removed reference to DateFormatSymbolsProvider but suggested getInstance().
    // be used instead in case Android supports it in future.
    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given locale. It is recommended that the
     * {@link #getInstance(Locale) getInstance} method is used instead.
     *
     * @param locale the desired locale
     * @see #getInstance(Locale)
     * @exception  java.util.MissingResourceException
     *             if the resources for the specified locale cannot be
     *             found or cannot be loaded.
     */
    public DateFormatSymbols(Locale locale)
    {
        initializeData(locale);
    }

    // Android-removed: unused private DateFormatSymbols(boolean) constructor.

    /**
     * Era strings. For example: "AD" and "BC".  An array of 2 strings,
     * indexed by <code>Calendar.BC</code> and <code>Calendar.AD</code>.
     * @serial
     */
    String eras[] = null;

    /**
     * Month strings. For example: "January", "February", etc.  An array
     * of 13 strings (some calendars have 13 months), indexed by
     * <code>Calendar.JANUARY</code>, <code>Calendar.FEBRUARY</code>, etc.
     * @serial
     */
    String months[] = null;

    /**
     * Short month strings. For example: "Jan", "Feb", etc.  An array of
     * 13 strings (some calendars have 13 months), indexed by
     * <code>Calendar.JANUARY</code>, <code>Calendar.FEBRUARY</code>, etc.

     * @serial
     */
    String shortMonths[] = null;

    /**
     * Weekday strings. For example: "Sunday", "Monday", etc.  An array
     * of 8 strings, indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * The element <code>weekdays[0]</code> is ignored.
     * @serial
     */
    String weekdays[] = null;

    /**
     * Short weekday strings. For example: "Sun", "Mon", etc.  An array
     * of 8 strings, indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * The element <code>shortWeekdays[0]</code> is ignored.
     * @serial
     */
    String shortWeekdays[] = null;

    /**
     * AM and PM strings. For example: "AM" and "PM".  An array of
     * 2 strings, indexed by <code>Calendar.AM</code> and
     * <code>Calendar.PM</code>.
     * @serial
     */
    String ampms[] = null;

    /**
     * Localized names of time zones in this locale.  This is a
     * two-dimensional array of strings of size <em>n</em> by <em>m</em>,
     * where <em>m</em> is at least 5.  Each of the <em>n</em> rows is an
     * entry containing the localized names for a single <code>TimeZone</code>.
     * Each such row contains (with <code>i</code> ranging from
     * 0..<em>n</em>-1):
     * <ul>
     * <li><code>zoneStrings[i][0]</code> - time zone ID</li>
     * <li><code>zoneStrings[i][1]</code> - long name of zone in standard
     * time</li>
     * <li><code>zoneStrings[i][2]</code> - short name of zone in
     * standard time</li>
     * <li><code>zoneStrings[i][3]</code> - long name of zone in daylight
     * saving time</li>
     * <li><code>zoneStrings[i][4]</code> - short name of zone in daylight
     * saving time</li>
     * </ul>
     * The zone ID is <em>not</em> localized; it's one of the valid IDs of
     * the {@link java.util.TimeZone TimeZone} class that are not
     * <a href="../util/TimeZone.html#CustomID">custom IDs</a>.
     * All other entries are localized names.
     * @see java.util.TimeZone
     * @serial
     */
    String zoneStrings[][] = null;

    /**
     * Indicates that zoneStrings is set externally with setZoneStrings() method.
     */
    transient boolean isZoneStringsSet = false;

    /**
     * Unlocalized date-time pattern characters. For example: 'y', 'd', etc.
     * All locales use the same these unlocalized pattern characters.
     */
    // Android-changed: Add 'c' (standalone day of week), 'b' (day period),.
    //   'B' (flexible day period)
    static final String  patternChars = "GyMdkHmsSEDFwWahKzZYuXLcbB";

    static final int PATTERN_ERA                  =  0; // G
    static final int PATTERN_YEAR                 =  1; // y
    static final int PATTERN_MONTH                =  2; // M
    static final int PATTERN_DAY_OF_MONTH         =  3; // d
    static final int PATTERN_HOUR_OF_DAY1         =  4; // k
    static final int PATTERN_HOUR_OF_DAY0         =  5; // H
    static final int PATTERN_MINUTE               =  6; // m
    static final int PATTERN_SECOND               =  7; // s
    static final int PATTERN_MILLISECOND          =  8; // S
    static final int PATTERN_DAY_OF_WEEK          =  9; // E
    static final int PATTERN_DAY_OF_YEAR          = 10; // D
    static final int PATTERN_DAY_OF_WEEK_IN_MONTH = 11; // F
    static final int PATTERN_WEEK_OF_YEAR         = 12; // w
    static final int PATTERN_WEEK_OF_MONTH        = 13; // W
    static final int PATTERN_AM_PM                = 14; // a
    static final int PATTERN_HOUR1                = 15; // h
    static final int PATTERN_HOUR0                = 16; // K
    static final int PATTERN_ZONE_NAME            = 17; // z
    static final int PATTERN_ZONE_VALUE           = 18; // Z
    static final int PATTERN_WEEK_YEAR            = 19; // Y
    static final int PATTERN_ISO_DAY_OF_WEEK      = 20; // u
    static final int PATTERN_ISO_ZONE             = 21; // X
    static final int PATTERN_MONTH_STANDALONE     = 22; // L
    // Android-added: Constant for standalone day of week.
    static final int PATTERN_STANDALONE_DAY_OF_WEEK = 23; // c
    // Android-added: Constant for pattern letter 'b', 'B'.
    static final int PATTERN_DAY_PERIOD = 24; // b
    static final int PATTERN_FLEXIBLE_DAY_PERIOD = 25; // B

    /**
     * Localized date-time pattern characters. For example, a locale may
     * wish to use 'u' rather than 'y' to represent years in its date format
     * pattern strings.
     * This string must be exactly 18 characters long, with the index of
     * the characters described by <code>DateFormat.ERA_FIELD</code>,
     * <code>DateFormat.YEAR_FIELD</code>, etc.  Thus, if the string were
     * "Xz...", then localized patterns would use 'X' for era and 'z' for year.
     * @serial
     */
    String  localPatternChars = null;

    /**
     * The locale which is used for initializing this DateFormatSymbols object.
     *
     * @since 1.6
     * @serial
     */
    Locale locale = null;

    /* use serialVersionUID from JDK 1.1.4 for interoperability */
    static final long serialVersionUID = -5987973545549424702L;

    // BEGIN Android-added: Android specific serialization code.
    // the internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.4
    // - 1 Android version that contains a whole bunch of new fields.
    static final int currentSerialVersion = 1;

    /**
     * The version of the serialized data on the stream.  Possible values:
     * <ul>
     * <li><b>0</b> or not present on stream: JDK 1.1.4.
     * <li><b>1</b> Android:
     * </ul>
     * When streaming out this class, the most recent format
     * and the highest allowable <code>serialVersionOnStream</code>
     * is written.
     * @serial
     * @since JDK1.1.4
     */
    private int serialVersionOnStream = currentSerialVersion;
    // END Android-added: Android specific serialization code.

    // BEGIN Android-added: Support for tiny and standalone field names.
    /**
     * Tiny month strings; "J", "F", "M" etc.
     *
     * @serial
     */
    private String[] tinyMonths;

    /**
     * Tiny weekday strings: "M", "F", "W" etc.
     *
     * @serial
     */
    private String[] tinyWeekdays;

    /**
     * Standalone month strings; "January", "February", "March" etc.
     *
     * @serial
     */
    private String[] standAloneMonths;

    /**
     * Short standalone month strings: "Jan", "Feb", "Mar" etc.
     *
     * @serial
     */
    private String[] shortStandAloneMonths;

    /**
     * Tiny standalone month strings: "J", "F", "M" etc.
     *
     * @serial
     */
    private String[] tinyStandAloneMonths;

    /**
     * Standalone weekday strings; "Monday", "Tuesday", "Wednesday" etc.
     *
     * @serial
     */
    private String[] standAloneWeekdays;

    /**
     * Short standalone weekday strings; "Mon", "Tue", "Wed" etc.
     *
     * @serial
     */
    private String[] shortStandAloneWeekdays;

    /**
     * Tiny standalone weekday strings; "M", "T", "W" etc.
     *
     * @serial
     */
    private String[] tinyStandAloneWeekdays;
    // END Android-added: Support for tiny and standalone field names.

    // Android-changed: Removed reference to DateFormatSymbolsProvider.
    /**
     * Returns an array of all locales for which the
     * <code>getInstance</code> methods of this class can return
     * localized instances.
     *
     * @return An array of locales for which localized
     *         <code>DateFormatSymbols</code> instances are available.
     * @since 1.6
     */
    public static Locale[] getAvailableLocales() {
        // Android-changed: No support for DateFormatSymbolsProvider.
        return ICU.getAvailableLocales();
    }

    // Android-changed: Removed reference to DateFormatSymbolsProvider.
    /**
     * Gets the <code>DateFormatSymbols</code> instance for the default
     * locale.
     * <p>This is equivalent to calling {@link #getInstance(Locale)
     *     getInstance(Locale.getDefault(Locale.Category.FORMAT))}.
     * @see java.util.Locale#getDefault(java.util.Locale.Category)
     * @see java.util.Locale.Category#FORMAT
     * @return a <code>DateFormatSymbols</code> instance.
     * @since 1.6
     */
    public static final DateFormatSymbols getInstance() {
        return getInstance(Locale.getDefault(Locale.Category.FORMAT));
    }

    // Android-changed: Removed reference to DateFormatSymbolsProvider.
    /**
     * Gets the <code>DateFormatSymbols</code> instance for the specified
     * locale.
     * @param locale the given locale.
     * @return a <code>DateFormatSymbols</code> instance.
     * @exception NullPointerException if <code>locale</code> is null
     * @since 1.6
     */
    public static final DateFormatSymbols getInstance(Locale locale) {
        // Android-changed: Removed used of DateFormatSymbolsProvider.
        return (DateFormatSymbols) getCachedInstance(locale).clone();
    }

    /**
     * Returns a DateFormatSymbols provided by a provider or found in
     * the cache. Note that this method returns a cached instance,
     * not its clone. Therefore, the instance should never be given to
     * an application.
     */
    static final DateFormatSymbols getInstanceRef(Locale locale) {
        // Android-changed: Removed used of DateFormatSymbolsProvider.
        return getCachedInstance(locale);
    }

    // BEGIN Android-changed: Replace getProviderInstance() with getCachedInstance().
    // Android removed support for DateFormatSymbolsProviders, but still caches DFS.
    // App compat change for b/159514442.
    /**
     * Returns a cached DateFormatSymbols if it's found in the
     * cache. Otherwise, this method returns a newly cached instance
     * for the given locale.
     */
    private static DateFormatSymbols getCachedInstance(Locale locale) {
        SoftReference<DateFormatSymbols> ref = cachedInstances.get(locale);
        DateFormatSymbols dfs;
        if (ref == null || (dfs = ref.get()) == null) {
            dfs = new DateFormatSymbols(locale);
            ref = new SoftReference<>(dfs);
            SoftReference<DateFormatSymbols> x = cachedInstances.putIfAbsent(locale, ref);
            if (x != null) {
                DateFormatSymbols y = x.get();
                if (y != null) {
                    dfs = y;
                } else {
                    // Replace the empty SoftReference with ref.
                    cachedInstances.put(locale, ref);
                }
            }
        }
        return dfs;
    }
    // END Android-changed: Replace getProviderInstance() with getCachedInstance().

    /**
     * Gets era strings. For example: "AD" and "BC".
     * @return the era strings.
     */
    public String[] getEras() {
        return Arrays.copyOf(eras, eras.length);
    }

    /**
     * Sets era strings. For example: "AD" and "BC".
     * @param newEras the new era strings.
     */
    public void setEras(String[] newEras) {
        eras = Arrays.copyOf(newEras, newEras.length);
        cachedHashCode = 0;
    }

    /**
     * Gets month strings. For example: "January", "February", etc.
     * An array with either 12 or 13 elements will be returned depending
     * on whether or not {@link java.util.Calendar#UNDECIMBER Calendar.UNDECIMBER}
     * is supported. Use
     * {@link java.util.Calendar#JANUARY Calendar.JANUARY},
     * {@link java.util.Calendar#FEBRUARY Calendar.FEBRUARY},
     * etc. to index the result array.
     *
     * <p>If the language requires different forms for formatting and
     * stand-alone usages, this method returns month names in the
     * formatting form. For example, the preferred month name for
     * January in the Czech language is <em>ledna</em> in the
     * formatting form, while it is <em>leden</em> in the stand-alone
     * form. This method returns {@code "ledna"} in this case. Refer
     * to the <a href="http://unicode.org/reports/tr35/#Calendar_Elements">
     * Calendar Elements in the Unicode Locale Data Markup Language
     * (LDML) specification</a> for more details.
     *
     * @implSpec This method returns 13 elements since
     * {@link java.util.Calendar#UNDECIMBER Calendar.UNDECIMBER} is supported.
     * @return the month strings.
     */
    public String[] getMonths() {
        return Arrays.copyOf(months, months.length);
    }

    /**
     * Sets month strings. For example: "January", "February", etc.
     * @param newMonths the new month strings. The array should
     * be indexed by {@link java.util.Calendar#JANUARY Calendar.JANUARY},
     * {@link java.util.Calendar#FEBRUARY Calendar.FEBRUARY}, etc.
     */
    public void setMonths(String[] newMonths) {
        months = Arrays.copyOf(newMonths, newMonths.length);
        cachedHashCode = 0;
    }

    /**
     * Gets short month strings. For example: "Jan", "Feb", etc.
     * An array with either 12 or 13 elements will be returned depending
     * on whether or not {@link java.util.Calendar#UNDECIMBER Calendar.UNDECIMBER}
     * is supported. Use
     * {@link java.util.Calendar#JANUARY Calendar.JANUARY},
     * {@link java.util.Calendar#FEBRUARY Calendar.FEBRUARY},
     * etc. to index the result array.
     *
     * <p>If the language requires different forms for formatting and
     * stand-alone usages, this method returns short month names in
     * the formatting form. For example, the preferred abbreviation
     * for January in the Catalan language is <em>de gen.</em> in the
     * formatting form, while it is <em>gen.</em> in the stand-alone
     * form. This method returns {@code "de gen."} in this case. Refer
     * to the <a href="http://unicode.org/reports/tr35/#Calendar_Elements">
     * Calendar Elements in the Unicode Locale Data Markup Language
     * (LDML) specification</a> for more details.
     *
     * @implSpec This method returns 13 elements since
     * {@link java.util.Calendar#UNDECIMBER Calendar.UNDECIMBER} is supported.
     * @return the short month strings.
     */
    public String[] getShortMonths() {
        return Arrays.copyOf(shortMonths, shortMonths.length);
    }

    /**
     * Sets short month strings. For example: "Jan", "Feb", etc.
     * @param newShortMonths the new short month strings. The array should
     * be indexed by {@link java.util.Calendar#JANUARY Calendar.JANUARY},
     * {@link java.util.Calendar#FEBRUARY Calendar.FEBRUARY}, etc.
     */
    public void setShortMonths(String[] newShortMonths) {
        shortMonths = Arrays.copyOf(newShortMonths, newShortMonths.length);
        cachedHashCode = 0;
    }

    /**
     * Gets weekday strings. For example: "Sunday", "Monday", etc.
     * @return the weekday strings. Use
     * {@link java.util.Calendar#SUNDAY Calendar.SUNDAY},
     * {@link java.util.Calendar#MONDAY Calendar.MONDAY}, etc. to index
     * the result array.
     */
    public String[] getWeekdays() {
        return Arrays.copyOf(weekdays, weekdays.length);
    }

    /**
     * Sets weekday strings. For example: "Sunday", "Monday", etc.
     * @param newWeekdays the new weekday strings. The array should
     * be indexed by {@link java.util.Calendar#SUNDAY Calendar.SUNDAY},
     * {@link java.util.Calendar#MONDAY Calendar.MONDAY}, etc.
     */
    public void setWeekdays(String[] newWeekdays) {
        weekdays = Arrays.copyOf(newWeekdays, newWeekdays.length);
        cachedHashCode = 0;
    }

    /**
     * Gets short weekday strings. For example: "Sun", "Mon", etc.
     * @return the short weekday strings. Use
     * {@link java.util.Calendar#SUNDAY Calendar.SUNDAY},
     * {@link java.util.Calendar#MONDAY Calendar.MONDAY}, etc. to index
     * the result array.
     */
    public String[] getShortWeekdays() {
        return Arrays.copyOf(shortWeekdays, shortWeekdays.length);
    }

    /**
     * Sets short weekday strings. For example: "Sun", "Mon", etc.
     * @param newShortWeekdays the new short weekday strings. The array should
     * be indexed by {@link java.util.Calendar#SUNDAY Calendar.SUNDAY},
     * {@link java.util.Calendar#MONDAY Calendar.MONDAY}, etc.
     */
    public void setShortWeekdays(String[] newShortWeekdays) {
        shortWeekdays = Arrays.copyOf(newShortWeekdays, newShortWeekdays.length);
        cachedHashCode = 0;
    }

    /**
     * Gets ampm strings. For example: "AM" and "PM".
     * @return the ampm strings.
     */
    public String[] getAmPmStrings() {
        return Arrays.copyOf(ampms, ampms.length);
    }

    /**
     * Sets ampm strings. For example: "AM" and "PM".
     * @param newAmpms the new ampm strings.
     */
    public void setAmPmStrings(String[] newAmpms) {
        ampms = Arrays.copyOf(newAmpms, newAmpms.length);
        cachedHashCode = 0;
    }

    // Android-changed: Removed reference to TimeZoneNameProvider.
    /**
     * Gets time zone strings.  Use of this method is discouraged; use
     * {@link java.util.TimeZone#getDisplayName() TimeZone.getDisplayName()}
     * instead.
     * <p>
     * The value returned is a
     * two-dimensional array of strings of size <em>n</em> by <em>m</em>,
     * where <em>m</em> is at least 5.  Each of the <em>n</em> rows is an
     * entry containing the localized names for a single <code>TimeZone</code>.
     * Each such row contains (with <code>i</code> ranging from
     * 0..<em>n</em>-1):
     * <ul>
     * <li><code>zoneStrings[i][0]</code> - time zone ID</li>
     * <li><code>zoneStrings[i][1]</code> - long name of zone in standard
     * time</li>
     * <li><code>zoneStrings[i][2]</code> - short name of zone in
     * standard time</li>
     * <li><code>zoneStrings[i][3]</code> - long name of zone in daylight
     * saving time</li>
     * <li><code>zoneStrings[i][4]</code> - short name of zone in daylight
     * saving time</li>
     * </ul>
     * The zone ID is <em>not</em> localized; it's one of the valid IDs of
     * the {@link java.util.TimeZone TimeZone} class that are not
     * <a href="../util/TimeZone.html#CustomID">custom IDs</a>.
     * All other entries are localized names.  If a zone does not implement
     * daylight saving time, the daylight saving time names should not be used.
     * <p>
     * If {@link #setZoneStrings(String[][]) setZoneStrings} has been called
     * on this <code>DateFormatSymbols</code> instance, then the strings
     * provided by that call are returned. Otherwise, the returned array
     * contains names provided by the runtime.
     *
     * @return the time zone strings.
     * @see #setZoneStrings(String[][])
     */
    public String[][] getZoneStrings() {
        return getZoneStringsImpl(true);
    }

    /**
     * Sets time zone strings.  The argument must be a
     * two-dimensional array of strings of size <em>n</em> by <em>m</em>,
     * where <em>m</em> is at least 5.  Each of the <em>n</em> rows is an
     * entry containing the localized names for a single <code>TimeZone</code>.
     * Each such row contains (with <code>i</code> ranging from
     * 0..<em>n</em>-1):
     * <ul>
     * <li><code>zoneStrings[i][0]</code> - time zone ID</li>
     * <li><code>zoneStrings[i][1]</code> - long name of zone in standard
     * time</li>
     * <li><code>zoneStrings[i][2]</code> - short name of zone in
     * standard time</li>
     * <li><code>zoneStrings[i][3]</code> - long name of zone in daylight
     * saving time</li>
     * <li><code>zoneStrings[i][4]</code> - short name of zone in daylight
     * saving time</li>
     * </ul>
     * The zone ID is <em>not</em> localized; it's one of the valid IDs of
     * the {@link java.util.TimeZone TimeZone} class that are not
     * <a href="../util/TimeZone.html#CustomID">custom IDs</a>.
     * All other entries are localized names.
     *
     * @param newZoneStrings the new time zone strings.
     * @exception IllegalArgumentException if the length of any row in
     *    <code>newZoneStrings</code> is less than 5
     * @exception NullPointerException if <code>newZoneStrings</code> is null
     * @see #getZoneStrings()
     */
    public void setZoneStrings(String[][] newZoneStrings) {
        String[][] aCopy = new String[newZoneStrings.length][];
        for (int i = 0; i < newZoneStrings.length; ++i) {
            int len = newZoneStrings[i].length;
            if (len < 5) {
                throw new IllegalArgumentException();
            }
            aCopy[i] = Arrays.copyOf(newZoneStrings[i], len);
        }
        zoneStrings = aCopy;
        isZoneStringsSet = true;
        // Android-changed: don't include zone strings in hashCode to avoid populating it.
        // cachedHashCode = 0;
    }

    /**
     * Gets localized date-time pattern characters. For example: 'u', 't', etc.
     * @return the localized date-time pattern characters.
     */
    public String getLocalPatternChars() {
        return localPatternChars;
    }

    /**
     * Sets localized date-time pattern characters. For example: 'u', 't', etc.
     * @param newLocalPatternChars the new localized date-time
     * pattern characters.
     */
    public void setLocalPatternChars(String newLocalPatternChars) {
        // Call toString() to throw an NPE in case the argument is null
        localPatternChars = newLocalPatternChars.toString();
        cachedHashCode = 0;
    }

    // BEGIN Android-added: Support for tiny and standalone field names.
    String[] getTinyMonths() {
        return tinyMonths;
    }

    String[] getStandAloneMonths() {
        return standAloneMonths;
    }

    String[] getShortStandAloneMonths() {
        return shortStandAloneMonths;
    }

    String[] getTinyStandAloneMonths() {
        return tinyStandAloneMonths;
    }

    String[] getTinyWeekdays() {
        return tinyWeekdays;
    }

    String[] getStandAloneWeekdays() {
        return standAloneWeekdays;
    }

    String[] getShortStandAloneWeekdays() {
        return shortStandAloneWeekdays;
    }

    String[] getTinyStandAloneWeekdays() {
        return tinyStandAloneWeekdays;
    }
    // END Android-added: Support for tiny and standalone field names.

    /**
     * Overrides Cloneable
     */
    public Object clone()
    {
        try
        {
            DateFormatSymbols other = (DateFormatSymbols)super.clone();
            copyMembers(this, other);
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Override hashCode.
     * Generates a hash code for the DateFormatSymbols object.
     */
    @Override
    public int hashCode() {
        int hashCode = cachedHashCode;
        if (hashCode == 0) {
            hashCode = 5;
            hashCode = 11 * hashCode + Arrays.hashCode(eras);
            hashCode = 11 * hashCode + Arrays.hashCode(months);
            hashCode = 11 * hashCode + Arrays.hashCode(shortMonths);
            hashCode = 11 * hashCode + Arrays.hashCode(weekdays);
            hashCode = 11 * hashCode + Arrays.hashCode(shortWeekdays);
            hashCode = 11 * hashCode + Arrays.hashCode(ampms);
            // Android-changed: Don't include zone strings in hashCode to avoid populating it.
            // hashCode = 11 * hashCode + Arrays.deepHashCode(getZoneStringsWrapper());
            hashCode = 11 * hashCode + Objects.hashCode(localPatternChars);
            if (hashCode != 0) {
                cachedHashCode = hashCode;
            }
        }

        return hashCode;
    }

    /**
     * Override equals
     */
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DateFormatSymbols that = (DateFormatSymbols) obj;
        // BEGIN Android-changed: Avoid populating zoneStrings just for the comparison, add fields.
        if (!(Arrays.equals(eras, that.eras)
                && Arrays.equals(months, that.months)
                && Arrays.equals(shortMonths, that.shortMonths)
                && Arrays.equals(tinyMonths, that.tinyMonths)
                && Arrays.equals(weekdays, that.weekdays)
                && Arrays.equals(shortWeekdays, that.shortWeekdays)
                && Arrays.equals(tinyWeekdays, that.tinyWeekdays)
                && Arrays.equals(standAloneMonths, that.standAloneMonths)
                && Arrays.equals(shortStandAloneMonths, that.shortStandAloneMonths)
                && Arrays.equals(tinyStandAloneMonths, that.tinyStandAloneMonths)
                && Arrays.equals(standAloneWeekdays, that.standAloneWeekdays)
                && Arrays.equals(shortStandAloneWeekdays, that.shortStandAloneWeekdays)
                && Arrays.equals(tinyStandAloneWeekdays, that.tinyStandAloneWeekdays)
                && Arrays.equals(ampms, that.ampms)
                && ((localPatternChars != null
                  && localPatternChars.equals(that.localPatternChars))
                 || (localPatternChars == null
                  && that.localPatternChars == null)))) {
            return false;
        }
        if (!isZoneStringsSet && !that.isZoneStringsSet && Objects.equals(locale, that.locale)) {
            return true;
        }
        return Arrays.deepEquals(getZoneStringsWrapper(), that.getZoneStringsWrapper());
        // END Android-changed: Avoid populating zoneStrings just for the comparison, add fields.
    }

    // =======================privates===============================

    /**
     * Useful constant for defining time zone offsets.
     */
    static final int millisPerHour = 60*60*1000;

    /**
     * Cache to hold DateFormatSymbols instances per Locale.
     */
    private static final ConcurrentMap<Locale, SoftReference<DateFormatSymbols>> cachedInstances
        = new ConcurrentHashMap<>(3);

    private transient int lastZoneIndex;

    /**
     * Cached hash code
     */
    transient volatile int cachedHashCode;

    // Android-changed: update comment to describe local modification.
    /**
     * Initializes this DateFormatSymbols with the locale data. This method uses
     * a cached DateFormatSymbols instance for the given locale if available. If
     * there's no cached one, this method populates this objects fields from an
     * appropriate LocaleData object. Note: zoneStrings isn't initialized in this method.
     */
    private void initializeData(Locale locale) {
        SoftReference<DateFormatSymbols> ref = cachedInstances.get(locale);
        DateFormatSymbols dfs;
        // Android-changed: invert cache presence check to simplify code flow.
        if (ref != null && (dfs = ref.get()) != null) {
            copyMembers(dfs, this);
            return;
        }

        // BEGIN Android-changed: Use ICU data and move cache handling to getCachedInstance().
        locale = LocaleData.mapInvalidAndNullLocales(locale);
        LocaleData localeData = LocaleData.get(locale);

        this.locale = locale;
        eras = localeData.eras;
        months = localeData.longMonthNames;
        shortMonths = localeData.shortMonthNames;
        ampms = localeData.amPm;
        localPatternChars = patternChars;

        weekdays = localeData.longWeekdayNames;
        shortWeekdays = localeData.shortWeekdayNames;

        initializeSupplementaryData(localeData);
        // END Android-changed: Use ICU data and move cache handling to getCachedInstance().
    }

    // Android-removed: toOneBasedArray(String[]).

    // BEGIN Android-added: initializeSupplementaryData(LocaleData) for tiny and standalone fields.
    private void initializeSupplementaryData(LocaleData localeData) {
        // Tiny weekdays and months.
        tinyMonths = localeData.tinyMonthNames;
        tinyWeekdays = localeData.tinyWeekdayNames;

        // Standalone month names.
        standAloneMonths = localeData.longStandAloneMonthNames;
        shortStandAloneMonths = localeData.shortStandAloneMonthNames;
        tinyStandAloneMonths = localeData.tinyStandAloneMonthNames;

        // Standalone weekdays.
        standAloneWeekdays = localeData.longStandAloneWeekdayNames;
        shortStandAloneWeekdays = localeData.shortStandAloneWeekdayNames;
        tinyStandAloneWeekdays = localeData.tinyStandAloneWeekdayNames;
    }
    // END Android-added: initializeSupplementaryData(LocaleData) for tiny and standalone fields.

    /**
     * Package private: used by SimpleDateFormat
     * Gets the index for the given time zone ID to obtain the time zone
     * strings for formatting. The time zone ID is just for programmatic
     * lookup. NOT LOCALIZED!!!
     * @param ID the given time zone ID.
     * @return the index of the given time zone ID.  Returns -1 if
     * the given time zone ID can't be located in the DateFormatSymbols object.
     * @see java.util.SimpleTimeZone
     */
    final int getZoneIndex(String ID) {
        String[][] zoneStrings = getZoneStringsWrapper();

        /*
         * getZoneIndex has been re-written for performance reasons. instead of
         * traversing the zoneStrings array every time, we cache the last used zone
         * index
         */
        if (lastZoneIndex < zoneStrings.length && ID.equals(zoneStrings[lastZoneIndex][0])) {
            return lastZoneIndex;
        }

        /* slow path, search entire list */
        for (int index = 0; index < zoneStrings.length; index++) {
            if (ID.equals(zoneStrings[index][0])) {
                lastZoneIndex = index;
                return index;
            }
        }

        return -1;
    }

    /**
     * Wrapper method to the getZoneStrings(), which is called from inside
     * the java.text package and not to mutate the returned arrays, so that
     * it does not need to create a defensive copy.
     */
    final String[][] getZoneStringsWrapper() {
        if (isSubclassObject()) {
            return getZoneStrings();
        } else {
            return getZoneStringsImpl(false);
        }
    }

    // BEGIN Android-changed: extract initialization of zoneStrings to separate method.
    private synchronized String[][] internalZoneStrings() {
        if (zoneStrings == null) {
            zoneStrings = TimeZoneNames.getZoneStrings(locale);
        }
        return zoneStrings;
    }
    // END Android-changed: extract initialization of zoneStrings to separate method.

    private String[][] getZoneStringsImpl(boolean needsCopy) {
        // Android-changed: use helper method to initialize zoneStrings.
        String[][] zoneStrings = internalZoneStrings();

        if (!needsCopy) {
            return zoneStrings;
        }

        int len = zoneStrings.length;
        String[][] aCopy = new String[len][];
        for (int i = 0; i < len; i++) {
            aCopy[i] = Arrays.copyOf(zoneStrings[i], zoneStrings[i].length);
        }
        return aCopy;
    }

    private boolean isSubclassObject() {
        return !getClass().getName().equals("java.text.DateFormatSymbols");
    }

    /**
     * Clones all the data members from the source DateFormatSymbols to
     * the target DateFormatSymbols.
     *
     * @param src the source DateFormatSymbols.
     * @param dst the target DateFormatSymbols.
     */
    private void copyMembers(DateFormatSymbols src, DateFormatSymbols dst)
    {
        dst.locale = src.locale;
        dst.eras = Arrays.copyOf(src.eras, src.eras.length);
        dst.months = Arrays.copyOf(src.months, src.months.length);
        dst.shortMonths = Arrays.copyOf(src.shortMonths, src.shortMonths.length);
        dst.weekdays = Arrays.copyOf(src.weekdays, src.weekdays.length);
        dst.shortWeekdays = Arrays.copyOf(src.shortWeekdays, src.shortWeekdays.length);
        dst.ampms = Arrays.copyOf(src.ampms, src.ampms.length);
        if (src.zoneStrings != null) {
            dst.zoneStrings = src.getZoneStringsImpl(true);
        } else {
            dst.zoneStrings = null;
        }
        dst.localPatternChars = src.localPatternChars;
        dst.cachedHashCode = 0;

        // BEGIN Android-added: Support for tiny and standalone field names.
        dst.tinyMonths = src.tinyMonths;
        dst.tinyWeekdays = src.tinyWeekdays;

        dst.standAloneMonths = src.standAloneMonths;
        dst.shortStandAloneMonths = src.shortStandAloneMonths;
        dst.tinyStandAloneMonths = src.tinyStandAloneMonths;

        dst.standAloneWeekdays = src.standAloneWeekdays;
        dst.shortStandAloneWeekdays = src.shortStandAloneWeekdays;
        dst.tinyStandAloneWeekdays = src.tinyStandAloneWeekdays;
        // END Android-added: Support for tiny and standalone field names.
    }

    // BEGIN Android-added: support reading non-Android serialized DFS.
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        if (serialVersionOnStream < 1) {
            LocaleData localeData = LocaleData.get(locale);
            initializeSupplementaryData(localeData);
        }

        serialVersionOnStream = currentSerialVersion;
    }
    // END Android-added: support reading non-Android serialized DFS.

    /**
     * Write out the default serializable data, after ensuring the
     * <code>zoneStrings</code> field is initialized in order to make
     * sure the backward compatibility.
     *
     * @since 1.6
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        // Android-changed: extract initialization of zoneStrings to separate method.
        internalZoneStrings();
        stream.defaultWriteObject();
    }
}
