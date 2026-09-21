import { render, fireEvent, waitFor } from '@testing-library/react'
import { screen } from 'shadow-dom-testing-library'
import App from './App'

describe('App', () => {
  beforeEach(() => {
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: vi.fn().mockImplementation(() => Promise.resolve()),
      },
      writable: true,
    })

    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
  })

  it('renders the starter heading and helper text', () => {
    render(<App />)

    expect(screen.getByText('Hello Material')).toBeInTheDocument()
    expect(
      screen.getByText('Enter a prompt to generate a Material app.'),
    ).toBeInTheDocument()
  })

  it('renders Getting Started section', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Getting Started' })).toBeInTheDocument()
    expect(
      screen.getByText(/To get started, ensure you have installed/),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Node.js' })).toBeInTheDocument()
  })

  it('renders Browser Tools instructions', () => {
    render(<App />)

    expect(screen.getByText(/Ensure browser tools are enabled/)).toBeInTheDocument()
  })

  it('renders Example Prompts section', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Example Prompts' })).toBeInTheDocument()
    expect(screen.getByShadowText('Prompt')).toBeInTheDocument()
    expect(screen.getByShadowText('Review')).toBeInTheDocument()
    expect(screen.getByShadowText('Prototype')).toBeInTheDocument()
  })

  it('renders theme toggle button', () => {
    render(<App />)

    expect(screen.getByShadowLabelText('Switch to dark mode')).toBeInTheDocument()
  })

  it('toggles theme on click', () => {
    render(<App />)

    const toggleButton = screen.getByShadowLabelText('Switch to dark mode')
    fireEvent.click(toggleButton)

    expect(screen.getByShadowLabelText('Switch to light mode')).toBeInTheDocument()
    expect(document.documentElement.style.colorScheme).toBe('dark')
  })

  it('shows snackbar when copy button is clicked', async () => {
    render(<App />)

    const copyButtons = screen.getAllByShadowText('Copy')
    fireEvent.click(copyButtons[0])

    expect(screen.getByText('Copied to the clipboard!')).toBeInTheDocument()

    // Wait for it to disappear
    await waitFor(() => {
      expect(screen.queryByText('Copied to the clipboard!')).not.toBeInTheDocument()
    }, { timeout: 2500 })
  })
it('initializes to dark mode if browser prefers dark', () => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: true,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })

    render(<App />)

    expect(screen.getByShadowLabelText('Switch to light mode')).toBeInTheDocument()
    expect(document.documentElement.style.colorScheme).toBe('dark')
  })
})
