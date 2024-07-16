// Grab course info from the table in the student portal
// Paste into console

const table = document.getElementsByClassName("admissionsinfo_table")[0].getElementsByTagName("tr");

const getCourseInfo = (table, sections) => {
    let courses = [];

    const regex = /([A-Z]{3,4}\d{5})\s*-\s*(.+?)Enrolled/;

    for (let i = 1; i < table.length; i++) {
        const row = table[i];
        const content = row.innerText.replace(/[\t\n]/g, '');
        const match = content.match(regex);

        if (match) {
            for (let i = 1; i <= sections; i++) {
                courses.push({
                    code: `${ match[1] } - Sec ${ i }`,
                    name: match[2]
                });
            }

        } else {
            console.log("Failed to match");
        }
    }

    return courses;
};


// can change number of sections
JSON.stringify(getCourseInfo(table, 4), null, 4);
