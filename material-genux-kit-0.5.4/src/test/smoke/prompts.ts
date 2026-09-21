/**
 * Smoke test prompt definitions.
 *
 * Each prompt targets a known regression area:
 * - travel-booking-form: historically triggered the "giant button" issue
 * - gmail-gemini-interface: historically struggled with font/typography usage
 */

export interface SmokePrompt {
  /** Short kebab-case identifier used in test names and logs. */
  name: string;
  /** The full design brief sent to the model. */
  text: string;
  /** gBreeze components we expect to see imported for this prompt. */
  expectedComponents: string[];
}

export const PROMPTS: SmokePrompt[] = [
  {
    name: 'travel-booking-form',
    text: 'Design the core booking form for an internal travel tool. The form should only include fields for origin, destination, and dates, with a "Search Flights" call-to-action.',
    expectedComponents: ['Button', 'TextField'],
  },
  {
    name: 'gmail-gemini-interface',
    text: 'Design a next-generation Gmail interface powered by a deep Gemini integration. Focus on AI-driven email composition tools (e.g., auto-drafting full replies), a proactive assistant that schedules meetings and creates tasks from email content, and a semantic search that understands complex queries.',
    expectedComponents: ['Button', 'Card', 'TopAppBar', 'SearchBar', 'IconButton'],
  },
];
