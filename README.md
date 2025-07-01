Remind Me Again
===============

Remind Me Again is a simple app for recurring reminders.


Acknowledgements
----------------

This app has been build almost entirely using AI. AI tools and
assistants used:

* ChatGPT (mostly GPT 4o)
* Claude Code (mostly Claude Sonnet 3.7)
* Cursor
* Gemini

The workflow evolved as development progressed, but generally the
approach follows this pattern:

1. Identify a change that needs to be made; it could be a bug, or app
   behavior that doesn't feel right.

2. Discuss the change with Gemini. Compare options. Read documentation.
   Update `doc/design.md`.

3. Take the updated `doc/design.md` and, if applicable, an edited
   version of Gemini's feedback to Cursor. Get questions from Cursor.
   Potentially query Gemini for answers. If appropriate, update
   `doc/design.md` to preserve clarifications or resolve ambiguities.
   Repeat until Cursor has no more questions.

4. Get Cursor to implement changes and to add and update tests.

5. Superficially glance through code using Android Studio, resolving
   warnings and accepting suggested tweaks.

6. Manually check.

7. Commit changes.

Code was manually edited very seldom, and it shows: It is peppered with
comments documenting "what" and giving suggestions. These have been
left, not only as artifacts of how the code was written, but also with
the thought that they might be helpful context for the next AI assistant
that encounters the code.


Installation
------------

This app is not yet complete. To try it out, clone this repository, open
it in Android Studio, connect your device, and run `MainActivity`.


YMMV
----

This is not a stable application. Do not put data in this app that you
will miss if it all gets lost.


Contributions
-------------

Contributions are welcome regardless of whether you are a human or not.
Open a pull request, and add **kaapstorm** as a reviewer, so that I am
more likely to be aware of your PR.


Bugs
----

I am pretty certain there are bugs. If you or an AI assistant guided by
you can fix them, brilliant! See "Contributions" above. If you can't,
honestly, I'd prefer not to know about them. My approach to bugs might
change in the future, but right now I don't have a lot of time to
maintain this app. It really was an exercise in greenfield AI-driven
software development. So for now and for the foreseeable future, if bugs
that you find annoy you, then this application might not be for you.
