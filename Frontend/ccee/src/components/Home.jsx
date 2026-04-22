const SUBJECTS = [
  {
    code: "JAVA",
    title: "Object Oriented Programming with Java",
    summary: "Syntax, data types, classes, objects, constructors, inheritance, interfaces, exceptions, and collections.",
  },
  {
    code: "DBMS",
    title: "Database Technologies",
    summary: "Design, ER modeling, SQL, joins, views, indexes, routines, triggers, transactions, optimization, PL/SQL, and NoSQL.",
  },
  {
    code: "CPP",
    title: "C++ Programming",
    summary: "Program structure, classes, constructors, inheritance, polymorphism, overloading, templates, exceptions, STL, files, and memory.",
  },
  {
    code: "WEB",
    title: "Web Programming Technologies",
    summary: "HTML5, CSS3, Bootstrap, JavaScript ES6, DOM, responsive design, AJAX, JSON, validation, hosting, and client-server basics.",
  },
  {
    code: "DSA",
    title: "Algorithms and Data Structures",
    summary: "Arrays, linked lists, stacks, queues, trees, graphs, sorting, searching, recursion, greedy, divide and conquer, dynamic programming, and complexity.",
  },
];

function Home({ loading, onStartTest, onViewAnalysis }) {
  return (
    <main className="dashboard">
      <section className="hero-panel">
        <div>
          <p className="eyebrow">Practice Dashboard</p>
          <h2>Choose a subject and launch a secure mock test.</h2>
          <p className="hero-copy">
            Each test session is issued by the backend, scored server-side, and
            tied to your authenticated account for analysis.
          </p>
        </div>
        <button className="button button-secondary" onClick={onViewAnalysis}>
          Open Performance Analysis
        </button>
      </section>

      <section className="subject-grid">
        {SUBJECTS.map((subject) => (
          <article className="panel-card subject-card" key={subject.code}>
            <p className="subject-code">{subject.code}</p>
            <h3>{subject.title}</h3>
            <p>{subject.summary}</p>
            <button
              className="button button-primary"
              disabled={loading}
              onClick={() => onStartTest(subject.code)}
            >
              {loading ? "Preparing..." : `Start ${subject.code} Test`}
            </button>
          </article>
        ))}
      </section>
    </main>
  );
}

export default Home;
